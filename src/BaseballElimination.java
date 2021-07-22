import edu.princeton.cs.algs4.FlowEdge;
import edu.princeton.cs.algs4.FlowNetwork;
import edu.princeton.cs.algs4.FordFulkerson;
import edu.princeton.cs.algs4.In;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

public class BaseballElimination {

    private final Map<String, Integer> teamsMap;
    private final int[] wins;
    private final int[] losses;
    private final int[] remains;
    private final int[][] games;
    private final int numOfCombinations;
    private final int numOfVertices;
    private LinkedList<String> eliminationSquad;
    private Map<Integer, Integer> teamVerticesMap;

    public BaseballElimination(String filename) {
        In in = new In(filename);
        int n = in.readInt();
        teamsMap = new HashMap<>();

        wins = new int[n];
        losses = new int[n];
        remains = new int[n];
        games = new int[n][n];
        int i = 0;
        while (i < n) {
            teamsMap.put(in.readString(), i);
            wins[i] = in.readInt();
            losses[i] = in.readInt();
            remains[i] = in.readInt();
            for (int j = 0; j < n; j++) {
                games[i][j] = in.readInt();
            }
            i++;
        }
        numOfCombinations = teamsMap.size() * (teamsMap.size() - 1) / 2;
        numOfVertices = numOfCombinations + (teamsMap.size() - 1) + 2;
    }

    public int numberOfTeams() {
        return teamsMap.size();
    }

    public Iterable<String> teams() {
        return teamsMap.keySet();
    }

    public int wins(String team) {
        validateTeams(team);
        return wins[teamsMap.get(team)];
    }

    public int losses(String team) {
        validateTeams(team);
        return losses[teamsMap.get(team)];
    }

    public int remaining(String team) {
        validateTeams(team);
        return remains[teamsMap.get(team)];
    }

    public int against(String team1, String team2) {
        validateTeams(team1, team2);
        return games[teamsMap.get(team1)][teamsMap.get(team2)];
    }

    public boolean isEliminated(String team) {
        validateTeams(team);
        int teamId = teamsMap.get(team);
        eliminationSquad = new LinkedList<>();

        if (isTrivialEliminated(teamId)) {
            return true;
        }

        FlowNetwork network = createFlowNetwork(teamId);
        FordFulkerson maxFlow = new FordFulkerson(network, 0, numOfVertices - 1);

        boolean eliminated = false;
        for (int i = 1; i < numOfCombinations + 1; i++) {
            if (maxFlow.inCut(i)) {
                eliminated = true;
                break;
            }
        }

        if (eliminated) {
            HashSet<Integer> teamNums = new HashSet<>();
            for (int i = numOfCombinations + 1; i < numOfCombinations + numberOfTeams() + 1; i++) {
                if (maxFlow.inCut(i)) {
                    teamNums.add(teamVerticesMap.get(i));
                }
            }
            for (Map.Entry<String, Integer> entry : teamsMap.entrySet()) {
                if (teamNums.contains(entry.getValue())) {
                    eliminationSquad.add(entry.getKey());
                }
            }
            return true;
        }
        return false;
    }

    private boolean isTrivialEliminated(int teamId) {
        int maxWins = 0;
        int maxInd = 0;
        for (int i = 0; i < numberOfTeams(); i++) {
            if (i == teamId) {
                continue;
            }
            if (wins[i] > maxWins) {
                maxWins = wins[i];
                maxInd = i;
            }
        }
        if (wins[teamId] + remains[teamId] < maxWins) {
            for (Map.Entry<String, Integer> entry : teamsMap.entrySet()) {
                if (entry.getValue() == maxInd) {
                    eliminationSquad.add(entry.getKey());
                }
            }
            return true;
        }
        return false;
    }

    private FlowNetwork createFlowNetwork(int teamId) {
        FlowNetwork fn = new FlowNetwork(numOfVertices);
        teamVerticesMap = new HashMap<>();
        int[][] combs = getCombinations(teamId);
        int k = 0;
        for (int i = 1; i <= numOfCombinations; i++) {
            fn.addEdge(new FlowEdge(0, i, games[combs[k][0]][combs[k][1]]));
            if (combs[k][0] > teamId) {
                fn.addEdge(new FlowEdge(i, numOfCombinations + combs[k][0], Double.POSITIVE_INFINITY));
                teamVerticesMap.put(numOfCombinations + combs[k][0], combs[k][0]);
            } else {
                fn.addEdge(new FlowEdge(i, numOfCombinations + 1 + combs[k][0], Double.POSITIVE_INFINITY));
                teamVerticesMap.put(numOfCombinations + 1 + combs[k][0], combs[k][0]);
            }
            if (combs[k][1] > teamId) {
                fn.addEdge(new FlowEdge(i, numOfCombinations + combs[k][1], Double.POSITIVE_INFINITY));
                teamVerticesMap.put(numOfCombinations + combs[k][1], combs[k][1]);
            } else {
                fn.addEdge(new FlowEdge(i, numOfCombinations + 1 + combs[k][1], Double.POSITIVE_INFINITY));
                teamVerticesMap.put(numOfCombinations + 1 + combs[k][1], combs[k][1]);
            }
            k++;
        }
        int j = 0;
        for (int i = numOfCombinations + 1; i < numOfVertices - 1; i++) {
            if (j == teamId) {
                j++;
            }
            fn.addEdge(new FlowEdge(i, numOfVertices - 1, wins[teamId] + remains[teamId] - wins[j]));
            j++;
        }
        return fn;
    }

    private int[][] getCombinations(int avoid) {
        int[] arr = new int[numberOfTeams()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = i;
        }
        int[][] combs = new int[numOfCombinations][2];
        int k = 0;
        for (int i = 0; i < arr.length - 1; i++) {
            for (int j = i + 1; j < arr.length; j++) {
                if (i == avoid || j == avoid) {
                    continue;
                }
                combs[k][0] = arr[i];
                combs[k][1] = arr[j];
                k++;
            }
        }
        return combs;
    }

    public Iterable<String> certificateOfElimination(String team) {
        validateTeams(team);
        if (isEliminated(team)) {
            return eliminationSquad;
        }
        return null;
    }

    private void validateTeams(String... teams) {
        for (String s : teams) {
            if (!teamsMap.containsKey(s)) {
                throw new IllegalArgumentException("Illegal team name");
            }
        }
    }

    public static void main(String[] args) {
        BaseballElimination division = new BaseballElimination(args[0]);
        for (String team : division.teams()) {
            if (division.isEliminated(team)) {
                System.out.print(team + " is eliminated by the subset R = { ");
                for (String t : division.certificateOfElimination(team)) {
                    System.out.print(t + " ");
                }
                System.out.println("}");
            } else {
                System.out.println(team + " is not eliminated");
            }
        }
    }
}