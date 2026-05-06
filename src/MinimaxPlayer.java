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

//----------------------------------------------
  // instance variables

  // the number of levels minimax will look ahead
  private int depth = 1;
  private Player minPlayer;

//----------------------------------------------
  // constructors

  /** Creates new MinimaxPlayer */
  public MinimaxPlayer(String name, int number, Player minPlayer) {
    super(name, number);

    this.minPlayer = minPlayer;
  }

//----------------------------------------------
  // instance methods

  /**
   * Get the number of levels that the Minimax Player is currently looking ahead.
   */
  public int getDepth() {
    return depth;
  }

  /**
   * Set the number of levels that the Minimax Player will look ahead when getMove
   * is next called
   */
  public void setDepth(int anInt) {
    depth = anInt;
  }

  /**
   * Passed a copy of the board, asked what move it would like to make.
   *
   * The MinimaxPlayer periodically polls the thread that makes this call to see
   * if it is interrupted. If it is the player returns null.
   *
   * Looks ahead depth moves.
   */
  public Move getMove(Board b) {
    MinimaxCalculator calc = new MinimaxCalculator(b, this, minPlayer);
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

  // the number of moves we have tried
  private int moveCount = 0;
  private long startTime;

  private Player minPlayer;
  private Player maxPlayer;
  private Board board;

  private final int MAX_POSSIBLE_STRENGTH;
  private final int MIN_POSSIBLE_STRENGTH;

//-------------------------------------------------------
  // constructors
  MinimaxCalculator(Board b, Player max, Player min) {
    board = b;
    maxPlayer = max;
    minPlayer = min;

    MAX_POSSIBLE_STRENGTH = board.getBoardStats().getMaxStrength();
    MIN_POSSIBLE_STRENGTH = board.getBoardStats().getMinStrength();
  }

//-------------------------------------------------------
  // instance methods

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
      System.out.println("Error, 0 depth in minumax player");
      Thread.dumpStack();
      return null;
    }

    Move[] moves = reorderMoves(board.getPossibleMoves(maxPlayer));
    int maxStrength = MIN_POSSIBLE_STRENGTH;
    int maxIndex = 0;

    for (int i = 0; i < moves.length; i++) {
      if (board.move(moves[i])) {
        moveCount++;

        int strength = expandMinNode(depth - 1, maxStrength, MAX_POSSIBLE_STRENGTH);
        if (strength > maxStrength) {
          maxStrength = strength;
          maxIndex = i;
        }
        board.undoLastMove();
      } // end if move made

      // if the thread has been interrupted, return immediately.
      if (Thread.currentThread().isInterrupted()) {
        return null;
      }

    } // end for all moves

    long stopTime = System.currentTimeMillis();
    System.out.print("MINIMAX: Number of moves tried:" + moveCount);
    System.out.println(" Time:" + (stopTime - startTime) + " milliseconds");

    return moves[maxIndex];

  }

  /**
   * A max node returns the max score of its descendents. parentMinimum is the
   * minumum score that the parent has already encountered. if we find a score
   * that is higher than this, we will return that score immediately rather than
   * continue to expand the tree, since the min node above us only cares if we are
   * lower than its current min score.
   */
  private int expandMaxNode(int depth, int alpha, int beta) {
    // base step
    if (depth == 0 || board.isGameOver()) {
      return board.getBoardStats().getStrength(maxPlayer);
    }

    // recursive step
    Move[] moves = reorderMoves(board.getPossibleMoves(maxPlayer));
    int maxStrength = MIN_POSSIBLE_STRENGTH;

    for (int i = 0; i < moves.length; i++) {
      if (board.move(moves[i])) {
        moveCount++;
        int strength = expandMinNode(depth - 1, alpha, beta);

        // FIXME: a-b prune
        // if (strength >= beta) {
        //   board.undoLastMove();
        //   return strength;
        // }
        if (strength > maxStrength) {
          maxStrength = strength;
        }
        if (strength > alpha) {
          alpha = strength;
        }
        board.undoLastMove();
      } // end if move made

    } // end for all moves

    return maxStrength;

  }// end expandMaxNode

  /**
   * The min node chooses the smallest of its descendents. parentMaximum is the
   * maximum that the parent max node has already found, if we find something
   * smaller than this, return immediatly, since the parent max node will choose
   * the greatest value it can find.
   */
  private int expandMinNode(int depth, int alpha, int beta) {
    // base step
    if (depth == 0 || board.isGameOver()) {
      return board.getBoardStats().getStrength(maxPlayer);
    }

    // recursive step
    Move[] moves = reorderMoves(board.getPossibleMoves(minPlayer));
    int minStrength = MAX_POSSIBLE_STRENGTH;

    for (int i = 0; i < moves.length; i++) {
      if (board.move(moves[i])) {
        moveCount++;
        int strength = expandMaxNode(depth - 1, alpha, beta);

        // FIXME: a-b prune
        // if (strength <= alpha) {
        //   board.undoLastMove();
        //   return strength;
        // }
        if (strength < minStrength) {
          minStrength = strength;
        }
        if (strength < beta) {
          beta = strength;
        }
        board.undoLastMove();
      } // end if move made

    } // end for all moves

    return minStrength;

  }// end expandMinNode

}