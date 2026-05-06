// MinimaxPlayer.java

/**
 *
 * @author Sean Bridges
 * @version 1.0
 *
 *          The MinimaxPlayer uses the minimax algorithm to determine what move
 *          it should make. Looks ahead depth moves. The number of moves will be
 *          on the order of n^depth, where n is the number of moves possible,
 *          and depth is the number of moves the engine is searching. Because of
 *          this the minimax player periodically polls the thread that calls
 *          getMove(..) to see if it was interrupted. It the thread is
 *          interrupted, the player returns null after it checks.
 */
public class MinimaxPlayer extends DefaultPlayer {

  public enum PruningMode { Q1, Q2, Q3, Q4, Q5A, Q5B }

//----------------------------------------------
  // instance variables

  private int depth = 1;
  private Player minPlayer;
  private PruningMode mode = PruningMode.Q1;

//----------------------------------------------
  // constructors

  /** Creates new MinimaxPlayer */
  public MinimaxPlayer(String name, int number, Player minPlayer) {
    super(name, number);
    this.minPlayer = minPlayer;
  }

//----------------------------------------------
  // instance methods

  public int getDepth() {
    return depth;
  }

  public void setDepth(int anInt) {
    depth = anInt;
  }

  public void setMode(PruningMode m) {
    mode = m;
  }

  public Move getMove(Board b) {
    MinimaxCalculator calc = new MinimaxCalculator(b, this, minPlayer, mode);
    return calc.calculateMove(depth);
  }

}// end MinimaxPlayer

/**
 * The MinimaxCalculator does the actual work of finding the minimax move. A new
 * calculator should be created each time a move is to be made. A calculator
 * should only be used once.
 */
final class MinimaxCalculator {

//-------------------------------------------------------
  // instance variables

  // middle-out column order for a 7-column board: centre first, then alternating outward
  private static final int[] COLUMN_ORDER = {3, 2, 4, 1, 5, 0, 6};

  private int moveCount = 0;
  private long startTime;

  private Player minPlayer;
  private Player maxPlayer;
  private Board board;

  private final int MAX_POSSIBLE_STRENGTH;
  private final int MIN_POSSIBLE_STRENGTH;

  // flags derived from PruningMode
  private final boolean usePruning;   // Q1,Q3,Q4,Q5B → true; Q2,Q5A → false
  private final boolean deepPruning;  // Q4,Q5B → true; else false
  private final boolean equalPruning; // Q3,Q4,Q5B → true; else false (strict >/<)
  private final boolean middleOut;    // Q5A,Q5B → true; else false

//-------------------------------------------------------
  // constructors
  MinimaxCalculator(Board b, Player max, Player min, MinimaxPlayer.PruningMode mode) {
    board = b;
    maxPlayer = max;
    minPlayer = min;

    MAX_POSSIBLE_STRENGTH = board.getBoardStats().getMaxStrength();
    MIN_POSSIBLE_STRENGTH = board.getBoardStats().getMinStrength();

    usePruning  = (mode != MinimaxPlayer.PruningMode.Q2 && mode != MinimaxPlayer.PruningMode.Q5A);
    deepPruning = (mode == MinimaxPlayer.PruningMode.Q4 || mode == MinimaxPlayer.PruningMode.Q5B);
    equalPruning = (mode == MinimaxPlayer.PruningMode.Q3 || mode == MinimaxPlayer.PruningMode.Q4
                    || mode == MinimaxPlayer.PruningMode.Q5B);
    middleOut   = (mode == MinimaxPlayer.PruningMode.Q5A || mode == MinimaxPlayer.PruningMode.Q5B);
  }

//-------------------------------------------------------
  // instance methods

  private Move[] getMoves(Player p) {
    Move[] moves = board.getPossibleMoves(p);
    if (middleOut) return reorderMoves(moves);
    return moves;
  }

  private Move[] reorderMoves(Move[] moves) {
    Move[] ordered = new Move[moves.length];
    for (int i = 0; i < COLUMN_ORDER.length; i++) {
      ordered[i] = moves[COLUMN_ORDER[i]];
    }
    return ordered;
  }

  /**
   * Calculate the move to be made.
   */
  public Move calculateMove(int depth) {
    startTime = System.currentTimeMillis();

    if (depth == 0) {
      System.out.println("Error, 0 depth in minimax player");
      Thread.dumpStack();
      return null;
    }

    Move[] moves = getMoves(maxPlayer);
    int maxStrength = MIN_POSSIBLE_STRENGTH;
    int alpha = MIN_POSSIBLE_STRENGTH;
    int maxIndex = 0;

    for (int i = 0; i < moves.length; i++) {
      if (board.move(moves[i])) {
        moveCount++;

        int childAlpha = deepPruning ? alpha : maxStrength;
        int strength = expandMinNode(depth - 1, childAlpha, MAX_POSSIBLE_STRENGTH);

        if (strength > maxStrength) {
          maxStrength = strength;
          maxIndex = i;
        }
        if (deepPruning && strength > alpha) alpha = strength;
        board.undoLastMove();
      }

      if (Thread.currentThread().isInterrupted()) return null;
    }

    long stopTime = System.currentTimeMillis();
    System.out.print("MINIMAX: Number of moves tried:" + moveCount);
    System.out.println(" Time:" + (stopTime - startTime) + " milliseconds");

    return moves[maxIndex];
  }

  /**
   * A max node returns the max score of its descendants.
   * alpha = best MAX can guarantee from ancestors; beta = MIN's current ceiling.
   */
  private int expandMaxNode(int depth, int alpha, int beta) {
    if (depth == 0 || board.isGameOver()) {
      return board.getBoardStats().getStrength(maxPlayer);
    }

    Move[] moves = getMoves(maxPlayer);
    int maxStrength = MIN_POSSIBLE_STRENGTH;

    for (int i = 0; i < moves.length; i++) {
      if (board.move(moves[i])) {
        moveCount++;

        // deep: pass inherited alpha/beta; shallow: pass only local values
        int childAlpha = deepPruning ? alpha       : maxStrength;
        int childBeta  = deepPruning ? beta        : MAX_POSSIBLE_STRENGTH;
        int strength = expandMinNode(depth - 1, childAlpha, childBeta);

        if (usePruning) {
          if (equalPruning ? (strength >= beta) : (strength > beta)) {
            board.undoLastMove();
            return strength;
          }
        }

        if (strength > maxStrength) maxStrength = strength;
        if (deepPruning && strength > alpha) alpha = strength;
        board.undoLastMove();
      }
    }

    return maxStrength;
  }

  /**
   * A min node returns the min score of its descendants.
   * alpha = MAX's global floor; beta = best MIN can guarantee from ancestors.
   */
  private int expandMinNode(int depth, int alpha, int beta) {
    if (depth == 0 || board.isGameOver()) {
      return board.getBoardStats().getStrength(maxPlayer);
    }

    Move[] moves = getMoves(minPlayer);
    int minStrength = MAX_POSSIBLE_STRENGTH;

    for (int i = 0; i < moves.length; i++) {
      if (board.move(moves[i])) {
        moveCount++;

        // deep: pass inherited alpha/beta; shallow: pass only local values
        int childAlpha = deepPruning ? alpha       : MIN_POSSIBLE_STRENGTH;
        int childBeta  = deepPruning ? beta        : minStrength;
        int strength = expandMaxNode(depth - 1, childAlpha, childBeta);

        if (usePruning) {
          if (equalPruning ? (strength <= alpha) : (strength < alpha)) {
            board.undoLastMove();
            return strength;
          }
        }

        if (strength < minStrength) minStrength = strength;
        if (deepPruning && strength < beta) beta = strength;
        board.undoLastMove();
      }
    }

    return minStrength;
  }

}
