package com.moritzsoft.renterspiele;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SchachActivity extends AppCompatActivity {

    // Piece encoding: 0=empty, positive=white, negative=black
    // 1=Pawn, 2=Knight, 3=Bishop, 4=Rook, 5=Queen, 6=King
    private int[][] board = new int[8][8];
    private boolean vsBot = true;
    private boolean whiteTurn = true;
    private int selectedRow = -1, selectedCol = -1;
    private List<int[]> validMoves = new ArrayList<>();
    private boolean gameOver = false;
    private boolean whiteKingMoved, blackKingMoved;
    private boolean whiteRookAMoved, whiteRookHMoved, blackRookAMoved, blackRookHMoved;
    private int enPassantCol = -1;
    private int lastMoveFromR = -1, lastMoveFromC = -1, lastMoveToR = -1, lastMoveToC = -1;

    // === Draw detection state ===
    private int halfMoveClock = 0; // moves since last pawn move or capture (for 50-move rule)
    private List<Long> positionHistory = new ArrayList<>(); // Zobrist hashes for threefold repetition

    // === Zobrist hashing ===
    private long[][][] zobristPieces = new long[2][7][64]; // [color][pieceType][square]
    private long zobristBlackToMove;
    private long[] zobristCastling = new long[4]; // WK, WQ, BK, BQ
    private long[] zobristEnPassant = new long[8]; // per file
    private long currentHash;

    private static final String[] WHITE_PIECES = {"", "♙", "♘", "♗", "♖", "♕", "♔"};
    private static final String[] BLACK_PIECES = {"", "♟", "♞", "♝", "♜", "♛", "♚"};

    // === Piece values in centipawns ===
    private static final int[] PIECE_VALUES = {0, 100, 320, 330, 500, 900, 20000};

    // === Piece-Square Tables (from white's perspective, row 0 = rank 8) ===
    private static final int[] PAWN_TABLE = {
         0,  0,  0,  0,  0,  0,  0,  0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
         5,  5, 10, 25, 25, 10,  5,  5,
         0,  0,  0, 20, 20,  0,  0,  0,
         5, -5,-10,  0,  0,-10, -5,  5,
         5, 10, 10,-20,-20, 10, 10,  5,
         0,  0,  0,  0,  0,  0,  0,  0
    };

    private static final int[] KNIGHT_TABLE = {
       -50,-40,-30,-30,-30,-30,-40,-50,
       -40,-20,  0,  0,  0,  0,-20,-40,
       -30,  0, 10, 15, 15, 10,  0,-30,
       -30,  5, 15, 20, 20, 15,  5,-30,
       -30,  0, 15, 20, 20, 15,  0,-30,
       -30,  5, 10, 15, 15, 10,  5,-30,
       -40,-20,  0,  5,  5,  0,-20,-40,
       -50,-40,-30,-30,-30,-30,-40,-50
    };

    private static final int[] BISHOP_TABLE = {
       -20,-10,-10,-10,-10,-10,-10,-20,
       -10,  0,  0,  0,  0,  0,  0,-10,
       -10,  0,  5, 10, 10,  5,  0,-10,
       -10,  5,  5, 10, 10,  5,  5,-10,
       -10,  0, 10, 10, 10, 10,  0,-10,
       -10, 10, 10, 10, 10, 10, 10,-10,
       -10,  5,  0,  0,  0,  0,  5,-10,
       -20,-10,-10,-10,-10,-10,-10,-20
    };

    private static final int[] ROOK_TABLE = {
         0,  0,  0,  0,  0,  0,  0,  0,
         5, 10, 10, 10, 10, 10, 10,  5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
         0,  0,  0,  5,  5,  0,  0,  0
    };

    private static final int[] QUEEN_TABLE = {
       -20,-10,-10, -5, -5,-10,-10,-20,
       -10,  0,  0,  0,  0,  0,  0,-10,
       -10,  0,  5,  5,  5,  5,  0,-10,
        -5,  0,  5,  5,  5,  5,  0, -5,
         0,  0,  5,  5,  5,  5,  0, -5,
       -10,  5,  5,  5,  5,  5,  0,-10,
       -10,  0,  5,  0,  0,  0,  0,-10,
       -20,-10,-10, -5, -5,-10,-10,-20
    };

    private static final int[] KING_MG_TABLE = {
       -30,-40,-40,-50,-50,-40,-40,-30,
       -30,-40,-40,-50,-50,-40,-40,-30,
       -30,-40,-40,-50,-50,-40,-40,-30,
       -30,-40,-40,-50,-50,-40,-40,-30,
       -20,-30,-30,-40,-40,-30,-30,-20,
       -10,-20,-20,-20,-20,-20,-20,-10,
        20, 20,  0,  0,  0,  0, 20, 20,
        20, 30, 10,  0,  0, 10, 30, 20
    };

    private static final int[] KING_EG_TABLE = {
       -50,-40,-30,-20,-20,-30,-40,-50,
       -30,-20,-10,  0,  0,-10,-20,-30,
       -30,-10, 20, 30, 30, 20,-10,-30,
       -30,-10, 30, 40, 40, 30,-10,-30,
       -30,-10, 30, 40, 40, 30,-10,-30,
       -30,-10, 20, 30, 30, 20,-10,-30,
       -30,-30,  0,  0,  0,  0,-30,-30,
       -50,-30,-30,-30,-30,-30,-30,-50
    };

    // Phase weights for tapered eval: Knight=1, Bishop=1, Rook=2, Queen=4
    private static final int[] PHASE_WEIGHT = {0, 0, 1, 1, 2, 4, 0};
    private static final int TOTAL_PHASE = 24; // 2*(2*1 + 2*2 + 1*4) per side = irrelevant, just 4+4+4+4+8=24

    // Bot tuning
    private static final int BOT_DEPTH = 3;
    private static final int NOISE_RANGE = 45; // ±45 centipawns
    private static final int BEST_MOVE_PERCENT = 68; // % chance to pick best move
    private static final int SECOND_MOVE_PERCENT = 22; // % chance for second best

    private Vibrator vibrator;
    private Handler handler = new Handler(Looper.getMainLooper());
    private BoardView boardView;
    private TextView txtStatus, txtInfo;
    private Random random = new Random();
    private boolean botThinking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        setContentView(R.layout.activity_schach);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        txtStatus = findViewById(R.id.txtStatus);
        txtInfo = findViewById(R.id.txtInfo);
        ((TextView) findViewById(R.id.txtGameTitle)).setText("Schach");

        initZobrist();

        findViewById(R.id.btnBot).setOnClickListener(v -> { vibrate(30); vsBot = true; startGame(); });
        findViewById(R.id.btnLocal).setOnClickListener(v -> { vibrate(30); vsBot = false; startGame(); });
        findViewById(R.id.btnBackMode).setOnClickListener(v -> { vibrate(30); finish(); });
        findViewById(R.id.btnBack).setOnClickListener(v -> { vibrate(30); finish(); });
        findViewById(R.id.btnNewGame).setOnClickListener(v -> { vibrate(30); startGame(); });
        findViewById(R.id.btnUndo).setVisibility(View.GONE);
        findViewById(R.id.btnHelp).setOnClickListener(v -> {
            vibrate(30);
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.help_schach_title))
                    .setMessage(getString(R.string.help_schach_text))
                    .setPositiveButton(getString(R.string.btn_understood), null).show();
        });
    }

    // =====================================================================
    //  ZOBRIST HASHING (for threefold repetition detection)
    // =====================================================================
    private void initZobrist() {
        Random zr = new Random(123456789L); // fixed seed for reproducibility
        for (int color = 0; color < 2; color++)
            for (int piece = 1; piece <= 6; piece++)
                for (int sq = 0; sq < 64; sq++)
                    zobristPieces[color][piece][sq] = zr.nextLong();
        zobristBlackToMove = zr.nextLong();
        for (int i = 0; i < 4; i++) zobristCastling[i] = zr.nextLong();
        for (int i = 0; i < 8; i++) zobristEnPassant[i] = zr.nextLong();
    }

    private long computeHash() {
        long h = 0;
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                int p = board[r][c];
                if (p != 0) {
                    int color = p > 0 ? 0 : 1;
                    int type = Math.abs(p);
                    h ^= zobristPieces[color][type][r * 8 + c];
                }
            }
        if (!whiteTurn) h ^= zobristBlackToMove;
        if (!whiteKingMoved && !whiteRookHMoved) h ^= zobristCastling[0];
        if (!whiteKingMoved && !whiteRookAMoved) h ^= zobristCastling[1];
        if (!blackKingMoved && !blackRookHMoved) h ^= zobristCastling[2];
        if (!blackKingMoved && !blackRookAMoved) h ^= zobristCastling[3];
        if (enPassantCol >= 0) h ^= zobristEnPassant[enPassantCol];
        return h;
    }

    // =====================================================================
    //  GAME START
    // =====================================================================
    private void startGame() {
        board = new int[8][8];
        int[] back = {4, 2, 3, 5, 6, 3, 2, 4};
        for (int c = 0; c < 8; c++) {
            board[7][c] = back[c]; board[6][c] = 1;
            board[1][c] = -1; board[0][c] = -back[c];
        }
        whiteTurn = true; gameOver = false;
        whiteKingMoved = blackKingMoved = false;
        whiteRookAMoved = whiteRookHMoved = blackRookAMoved = blackRookHMoved = false;
        enPassantCol = -1;
        lastMoveFromR = lastMoveFromC = lastMoveToR = lastMoveToC = -1;
        selectedRow = -1; selectedCol = -1; validMoves.clear();
        halfMoveClock = 0;
        positionHistory.clear();
        currentHash = computeHash();
        positionHistory.add(currentHash);
        botThinking = false;

        findViewById(R.id.modeScreen).setVisibility(View.GONE);
        findViewById(R.id.gameScreen).setVisibility(View.VISIBLE);
        FrameLayout container = findViewById(R.id.boardContainer);
        container.removeAllViews();
        boardView = new BoardView(this);
        container.addView(boardView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        updateStatus();
    }

    // =====================================================================
    //  BASIC HELPERS
    // =====================================================================
    private int pieceType(int p) { return p < 0 ? -p : p; }
    private boolean isWhitePiece(int p) { return p > 0; }
    private boolean inBounds(int r, int c) { return r >= 0 && r < 8 && c >= 0 && c < 8; }

    private int[] findKing(boolean white) {
        int val = white ? 6 : -6;
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (board[r][c] == val) return new int[]{r, c};
        return null;
    }

    // =====================================================================
    //  ATTACK DETECTION
    // =====================================================================
    private boolean isAttackedBy(int r, int c, boolean byWhite) {
        // Pawn attacks
        int pawnDir = byWhite ? 1 : -1;
        int pawn = byWhite ? 1 : -1;
        for (int dc : new int[]{-1, 1}) {
            int pr = r + pawnDir, pc = c + dc;
            if (inBounds(pr, pc) && board[pr][pc] == pawn) return true;
        }
        // Knight
        int knight = byWhite ? 2 : -2;
        int[][] knightD = {{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}};
        for (int[] d : knightD)
            if (inBounds(r+d[0], c+d[1]) && board[r+d[0]][c+d[1]] == knight) return true;
        // Bishop/Queen diagonals
        int bishop = byWhite ? 3 : -3;
        int queen = byWhite ? 5 : -5;
        int[][] diagDirs = {{-1,-1},{-1,1},{1,-1},{1,1}};
        for (int[] d : diagDirs) {
            int nr = r+d[0], nc = c+d[1];
            while (inBounds(nr, nc)) {
                if (board[nr][nc] != 0) {
                    if (board[nr][nc] == bishop || board[nr][nc] == queen) return true;
                    break;
                }
                nr += d[0]; nc += d[1];
            }
        }
        // Rook/Queen straights
        int rook = byWhite ? 4 : -4;
        int[][] straightDirs = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : straightDirs) {
            int nr = r+d[0], nc = c+d[1];
            while (inBounds(nr, nc)) {
                if (board[nr][nc] != 0) {
                    if (board[nr][nc] == rook || board[nr][nc] == queen) return true;
                    break;
                }
                nr += d[0]; nc += d[1];
            }
        }
        // King
        int king = byWhite ? 6 : -6;
        for (int dr = -1; dr <= 1; dr++)
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                if (inBounds(r+dr, c+dc) && board[r+dr][c+dc] == king) return true;
            }
        return false;
    }

    private boolean isInCheck(boolean whiteKing) {
        int[] pos = findKing(whiteKing);
        if (pos == null) return true;
        return isAttackedBy(pos[0], pos[1], !whiteKing);
    }

    // =====================================================================
    //  MOVE GENERATION
    // =====================================================================
    private List<int[]> getPseudoMoves(int r, int c) {
        List<int[]> moves = new ArrayList<>();
        int piece = board[r][c];
        if (piece == 0) return moves;
        boolean white = piece > 0;
        int type = pieceType(piece);

        switch (type) {
            case 1: { // Pawn
                int dir = white ? -1 : 1;
                int startRow = white ? 6 : 1;
                // Forward
                if (inBounds(r+dir, c) && board[r+dir][c] == 0) {
                    moves.add(new int[]{r+dir, c});
                    if (r == startRow && board[r+2*dir][c] == 0)
                        moves.add(new int[]{r+2*dir, c});
                }
                // Captures
                for (int dc : new int[]{-1, 1}) {
                    int nr = r+dir, nc = c+dc;
                    if (!inBounds(nr, nc)) continue;
                    if (board[nr][nc] != 0 && isWhitePiece(board[nr][nc]) != white)
                        moves.add(new int[]{nr, nc});
                    // En passant
                    if (nr == (white ? 2 : 5) && nc == enPassantCol && board[nr][nc] == 0)
                        moves.add(new int[]{nr, nc});
                }
                break;
            }
            case 2: { // Knight
                int[][] d = {{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}};
                for (int[] dd : d) {
                    int nr = r+dd[0], nc = c+dd[1];
                    if (inBounds(nr, nc) && (board[nr][nc] == 0 || isWhitePiece(board[nr][nc]) != white))
                        moves.add(new int[]{nr, nc});
                }
                break;
            }
            case 3: addSliding(moves, r, c, white, new int[][]{{-1,-1},{-1,1},{1,-1},{1,1}}); break;
            case 4: addSliding(moves, r, c, white, new int[][]{{-1,0},{1,0},{0,-1},{0,1}}); break;
            case 5: addSliding(moves, r, c, white, new int[][]{{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}}); break;
            case 6: { // King
                for (int dr = -1; dr <= 1; dr++)
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0) continue;
                        int nr = r+dr, nc = c+dc;
                        if (inBounds(nr, nc) && (board[nr][nc] == 0 || isWhitePiece(board[nr][nc]) != white))
                            moves.add(new int[]{nr, nc});
                    }
                // Castling
                if (white && !whiteKingMoved && r == 7 && c == 4 && !isInCheck(true)) {
                    if (!whiteRookHMoved && board[7][5]==0 && board[7][6]==0 && board[7][7]==4
                            && !isAttackedBy(7,5,false) && !isAttackedBy(7,6,false))
                        moves.add(new int[]{7,6});
                    if (!whiteRookAMoved && board[7][3]==0 && board[7][2]==0 && board[7][1]==0 && board[7][0]==4
                            && !isAttackedBy(7,3,false) && !isAttackedBy(7,2,false))
                        moves.add(new int[]{7,2});
                }
                if (!white && !blackKingMoved && r == 0 && c == 4 && !isInCheck(false)) {
                    if (!blackRookHMoved && board[0][5]==0 && board[0][6]==0 && board[0][7]==-4
                            && !isAttackedBy(0,5,true) && !isAttackedBy(0,6,true))
                        moves.add(new int[]{0,6});
                    if (!blackRookAMoved && board[0][3]==0 && board[0][2]==0 && board[0][1]==0 && board[0][0]==-4
                            && !isAttackedBy(0,3,true) && !isAttackedBy(0,2,true))
                        moves.add(new int[]{0,2});
                }
                break;
            }
        }
        return moves;
    }

    private void addSliding(List<int[]> moves, int r, int c, boolean white, int[][] dirs) {
        for (int[] d : dirs) {
            int nr = r+d[0], nc = c+d[1];
            while (inBounds(nr, nc)) {
                if (board[nr][nc] == 0) { moves.add(new int[]{nr, nc}); }
                else {
                    if (isWhitePiece(board[nr][nc]) != white) moves.add(new int[]{nr, nc});
                    break;
                }
                nr += d[0]; nc += d[1];
            }
        }
    }

    private List<int[]> getLegalMoves(int r, int c) {
        List<int[]> pseudo = getPseudoMoves(r, c);
        List<int[]> legal = new ArrayList<>();
        boolean white = board[r][c] > 0;
        int piece = board[r][c];

        for (int[] m : pseudo) {
            int captured = board[m[0]][m[1]];
            if (pieceType(captured) == 6) continue;

            board[m[0]][m[1]] = piece;
            board[r][c] = 0;

            int epCaptured = 0;
            int epR = -1, epC = -1;
            if (pieceType(piece) == 1 && m[1] != c && captured == 0) {
                epR = r; epC = m[1];
                epCaptured = board[epR][epC];
                board[epR][epC] = 0;
            }

            boolean kingSafe = !isInCheck(white);

            board[r][c] = piece;
            board[m[0]][m[1]] = captured;
            if (epR >= 0) board[epR][epC] = epCaptured;

            if (kingSafe) legal.add(m);
        }
        return legal;
    }

    private boolean hasAnyLegalMove(boolean white) {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (board[r][c] != 0 && isWhitePiece(board[r][c]) == white)
                    if (!getLegalMoves(r, c).isEmpty()) return true;
        return false;
    }

    // Get all legal moves for a color as [fromR, fromC, toR, toC]
    private List<int[]> getAllLegalMoves(boolean white) {
        List<int[]> all = new ArrayList<>();
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (board[r][c] != 0 && isWhitePiece(board[r][c]) == white)
                    for (int[] m : getLegalMoves(r, c))
                        all.add(new int[]{r, c, m[0], m[1]});
        return all;
    }

    // =====================================================================
    //  DRAW DETECTION
    // =====================================================================
    private boolean isInsufficientMaterial() {
        List<Integer> whitePieces = new ArrayList<>();
        List<Integer> blackPieces = new ArrayList<>();
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                int p = board[r][c];
                if (p > 0 && p != 6) whitePieces.add(p);
                if (p < 0 && -p != 6) blackPieces.add(-p);
            }

        int wCount = whitePieces.size();
        int bCount = blackPieces.size();

        // K vs K
        if (wCount == 0 && bCount == 0) return true;
        // K+B vs K or K+N vs K
        if (wCount == 0 && bCount == 1 && (blackPieces.get(0) == 2 || blackPieces.get(0) == 3)) return true;
        if (bCount == 0 && wCount == 1 && (whitePieces.get(0) == 2 || whitePieces.get(0) == 3)) return true;
        // K+B vs K+B same color bishops
        if (wCount == 1 && bCount == 1 && whitePieces.get(0) == 3 && blackPieces.get(0) == 3) {
            int[] wb = findPiece(3, true);
            int[] bb = findPiece(3, false);
            if (wb != null && bb != null) {
                boolean wLight = (wb[0] + wb[1]) % 2 == 0;
                boolean bLight = (bb[0] + bb[1]) % 2 == 0;
                if (wLight == bLight) return true;
            }
        }
        return false;
    }

    private int[] findPiece(int type, boolean white) {
        int val = white ? type : -type;
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (board[r][c] == val) return new int[]{r, c};
        return null;
    }

    private boolean isThreefoldRepetition() {
        if (positionHistory.size() < 5) return false;
        int count = 0;
        for (long h : positionHistory)
            if (h == currentHash) count++;
        return count >= 3;
    }

    private boolean isFiftyMoveRule() {
        return halfMoveClock >= 100; // 100 half-moves = 50 full moves
    }

    // =====================================================================
    //  CELL TAP & MOVE EXECUTION
    // =====================================================================
    private void onCellTapped(int row, int col) {
        if (gameOver || botThinking) return;
        if (vsBot && !whiteTurn) return;

        if (selectedRow >= 0) {
            for (int[] m : validMoves) {
                if (m[0] == row && m[1] == col) {
                    int piece = board[selectedRow][selectedCol];
                    int promoRow = isWhitePiece(piece) ? 0 : 7;
                    // Check if this is a pawn promotion move
                    if (pieceType(piece) == 1 && row == promoRow) {
                        showPromotionDialog(selectedRow, selectedCol, row, col, isWhitePiece(piece));
                    } else {
                        executeMove(selectedRow, selectedCol, row, col, 0);
                    }
                    return;
                }
            }
        }

        if (board[row][col] != 0 && isWhitePiece(board[row][col]) == whiteTurn) {
            selectedRow = row; selectedCol = col;
            validMoves = getLegalMoves(row, col);
            vibrate(20);
            boardView.invalidate();
        }
    }

    private void showPromotionDialog(int fromR, int fromC, int toR, int toC, boolean white) {
        String[] options = {"♕ Dame", "♖ Turm", "♗ Läufer", "♘ Springer"};
        int[] promoTypes = {5, 4, 3, 2};
        new AlertDialog.Builder(this)
                .setTitle("Bauernumwandlung")
                .setItems(options, (dialog, which) -> {
                    executeMove(fromR, fromC, toR, toC, promoTypes[which]);
                })
                .setCancelable(false)
                .show();
    }

    /**
     * Execute a move on the real board. promoType: 0 = auto-queen for bot / no promo,
     * 2/3/4/5 = specific promotion piece type.
     */
    private void executeMove(int fromR, int fromC, int toR, int toC, int promoType) {
        int piece = board[fromR][fromC];
        int captured = board[toR][toC];
        boolean isPawnMove = pieceType(piece) == 1;
        boolean isCapture = captured != 0;

        // En passant capture
        if (isPawnMove && toC != fromC && captured == 0) {
            board[fromR][toC] = 0;
            isCapture = true;
        }

        board[toR][toC] = piece;
        board[fromR][fromC] = 0;

        // Castling rook
        if (pieceType(piece) == 6 && Math.abs(toC - fromC) == 2) {
            if (toC == 6) { board[fromR][5] = board[fromR][7]; board[fromR][7] = 0; }
            if (toC == 2) { board[fromR][3] = board[fromR][0]; board[fromR][0] = 0; }
        }

        // En passant tracking
        enPassantCol = -1;
        if (isPawnMove && Math.abs(toR - fromR) == 2) {
            enPassantCol = fromC;
        }

        // Castling rights
        if (piece == 6) whiteKingMoved = true;
        if (piece == -6) blackKingMoved = true;
        if (fromR == 7 && fromC == 0) whiteRookAMoved = true;
        if (fromR == 7 && fromC == 7) whiteRookHMoved = true;
        if (fromR == 0 && fromC == 0) blackRookAMoved = true;
        if (fromR == 0 && fromC == 7) blackRookHMoved = true;
        // Also lose castling rights if rook is captured
        if (toR == 7 && toC == 0) whiteRookAMoved = true;
        if (toR == 7 && toC == 7) whiteRookHMoved = true;
        if (toR == 0 && toC == 0) blackRookAMoved = true;
        if (toR == 0 && toC == 7) blackRookHMoved = true;

        // Pawn promotion
        if (isPawnMove && (toR == 0 || toR == 7)) {
            int promoPiece = promoType > 0 ? promoType : 5; // default queen
            board[toR][toC] = piece > 0 ? promoPiece : -promoPiece;
        }

        // Update 50-move clock
        if (isPawnMove || isCapture) {
            halfMoveClock = 0;
        } else {
            halfMoveClock++;
        }

        lastMoveFromR = fromR; lastMoveFromC = fromC;
        lastMoveToR = toR; lastMoveToC = toC;

        selectedRow = -1; selectedCol = -1; validMoves.clear();
        vibrate(30);
        whiteTurn = !whiteTurn;

        // Update hash and position history
        currentHash = computeHash();
        positionHistory.add(currentHash);

        boardView.invalidate();
        checkGameState();
        updateStatus();

        if (vsBot && !whiteTurn && !gameOver) {
            botThinking = true;
            updateStatus();
            handler.postDelayed(this::botMove, 400);
        }
    }

    // =====================================================================
    //  GAME STATE CHECK (checkmate, stalemate, draws)
    // =====================================================================
    private void checkGameState() {
        // Check insufficient material
        if (isInsufficientMaterial()) {
            gameOver = true;
            txtStatus.setText("Unentschieden – Ungenügendes Material!");
            vibrate(500);
            return;
        }

        // Check 50-move rule
        if (isFiftyMoveRule()) {
            gameOver = true;
            txtStatus.setText("Unentschieden – 50-Züge-Regel!");
            vibrate(500);
            return;
        }

        // Check threefold repetition
        if (isThreefoldRepetition()) {
            gameOver = true;
            txtStatus.setText("Unentschieden – Dreifache Stellungswiederholung!");
            vibrate(500);
            return;
        }

        boolean currentInCheck = isInCheck(whiteTurn);
        boolean hasMove = hasAnyLegalMove(whiteTurn);

        if (!hasMove) {
            gameOver = true;
            if (currentInCheck) {
                txtStatus.setText(whiteTurn ? "Schachmatt! Schwarz gewinnt!" : "Schachmatt! Weiß gewinnt!");
            } else {
                txtStatus.setText("Patt! Unentschieden.");
            }
            vibrate(500);
        } else if (currentInCheck) {
            txtStatus.setText((whiteTurn ? "Weiß" : "Schwarz") + " steht im Schach!");
        }
    }

    // =====================================================================
    //  EVALUATION FUNCTION
    // =====================================================================
    private int getPstValue(int type, int sq, boolean isEndgame) {
        switch (type) {
            case 1: return PAWN_TABLE[sq];
            case 2: return KNIGHT_TABLE[sq];
            case 3: return BISHOP_TABLE[sq];
            case 4: return ROOK_TABLE[sq];
            case 5: return QUEEN_TABLE[sq];
            case 6: return isEndgame ? KING_EG_TABLE[sq] : KING_MG_TABLE[sq];
            default: return 0;
        }
    }

    private int getGamePhase() {
        int phase = 0;
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                int p = board[r][c];
                if (p != 0) phase += PHASE_WEIGHT[pieceType(p)];
            }
        return Math.min(phase, TOTAL_PHASE);
    }

    /**
     * Evaluate the board. Positive = white advantage, negative = black advantage.
     * Uses tapered eval blending middlegame and endgame king tables.
     */
    private int evaluateBoard() {
        int phase = getGamePhase();
        int mgScore = 0, egScore = 0;

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                int p = board[r][c];
                if (p == 0) continue;
                int type = pieceType(p);
                int sq = r * 8 + c;
                int material = PIECE_VALUES[type];

                if (p > 0) { // White
                    mgScore += material + getPstValue(type, sq, false);
                    egScore += material + getPstValue(type, sq, true);
                } else { // Black — flip the square
                    int flippedSq = (7 - r) * 8 + c;
                    mgScore -= material + getPstValue(type, flippedSq, false);
                    egScore -= material + getPstValue(type, flippedSq, true);
                }
            }
        }

        // Bonus: bishop pair
        boolean whiteBishopPair = countPiece(3, true) >= 2;
        boolean blackBishopPair = countPiece(3, false) >= 2;
        if (whiteBishopPair) { mgScore += 30; egScore += 50; }
        if (blackBishopPair) { mgScore -= 30; egScore -= 50; }

        // Penalty: doubled pawns
        for (int c = 0; c < 8; c++) {
            int wp = 0, bp = 0;
            for (int r = 0; r < 8; r++) {
                if (board[r][c] == 1) wp++;
                if (board[r][c] == -1) bp++;
            }
            if (wp > 1) { mgScore -= 10 * (wp - 1); egScore -= 15 * (wp - 1); }
            if (bp > 1) { mgScore += 10 * (bp - 1); egScore += 15 * (bp - 1); }
        }

        // Penalty: isolated pawns
        for (int c = 0; c < 8; c++) {
            boolean wPawnOnFile = false, bPawnOnFile = false;
            for (int r = 0; r < 8; r++) {
                if (board[r][c] == 1) wPawnOnFile = true;
                if (board[r][c] == -1) bPawnOnFile = true;
            }
            if (wPawnOnFile) {
                boolean hasNeighbor = false;
                for (int nc : new int[]{c-1, c+1})
                    if (nc >= 0 && nc < 8)
                        for (int r = 0; r < 8; r++)
                            if (board[r][nc] == 1) hasNeighbor = true;
                if (!hasNeighbor) { mgScore -= 15; egScore -= 20; }
            }
            if (bPawnOnFile) {
                boolean hasNeighbor = false;
                for (int nc : new int[]{c-1, c+1})
                    if (nc >= 0 && nc < 8)
                        for (int r = 0; r < 8; r++)
                            if (board[r][nc] == -1) hasNeighbor = true;
                if (!hasNeighbor) { mgScore += 15; egScore += 20; }
            }
        }

        // Bonus: passed pawns
        int[] passedBonus = {0, 10, 20, 40, 60, 100, 150, 0};
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                if (board[r][c] == 1) { // white pawn
                    boolean passed = true;
                    for (int rr = r - 1; rr >= 0 && passed; rr--)
                        for (int cc = Math.max(0, c-1); cc <= Math.min(7, c+1); cc++)
                            if (board[rr][cc] == -1) passed = false;
                    if (passed) {
                        int rank = 7 - r; // rank from white's perspective (0-7)
                        egScore += passedBonus[rank];
                        mgScore += passedBonus[rank] / 2;
                    }
                }
                if (board[r][c] == -1) { // black pawn
                    boolean passed = true;
                    for (int rr = r + 1; rr < 8 && passed; rr++)
                        for (int cc = Math.max(0, c-1); cc <= Math.min(7, c+1); cc++)
                            if (board[rr][cc] == 1) passed = false;
                    if (passed) {
                        int rank = r; // rank from black's perspective
                        egScore -= passedBonus[rank];
                        mgScore -= passedBonus[rank] / 2;
                    }
                }
            }

        // Tapered evaluation
        int score = (mgScore * phase + egScore * (TOTAL_PHASE - phase)) / TOTAL_PHASE;
        return score;
    }

    private int countPiece(int type, boolean white) {
        int val = white ? type : -type;
        int count = 0;
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (board[r][c] == val) count++;
        return count;
    }

    // =====================================================================
    //  MINIMAX (Negamax) WITH ALPHA-BETA PRUNING
    // =====================================================================

    // Lightweight make/unmake for search (no UI updates, no history)
    private static class UndoInfo {
        int fromR, fromC, toR, toC;
        int movedPiece, capturedPiece;
        int epCapturedPiece, epR, epC;
        int prevEnPassantCol;
        boolean prevWKM, prevBKM, prevWRA, prevWRH, prevBRA, prevBRH;
        int prevHalfMoveClock;
        int rookFromC, rookToC; // for castling rook undo
        boolean wasCastling;
        int promotedTo; // 0 if no promotion
    }

    private UndoInfo makeSearchMove(int fromR, int fromC, int toR, int toC, int promoType) {
        UndoInfo u = new UndoInfo();
        u.fromR = fromR; u.fromC = fromC; u.toR = toR; u.toC = toC;
        u.movedPiece = board[fromR][fromC];
        u.capturedPiece = board[toR][toC];
        u.epR = -1; u.epC = -1; u.epCapturedPiece = 0;
        u.prevEnPassantCol = enPassantCol;
        u.prevWKM = whiteKingMoved; u.prevBKM = blackKingMoved;
        u.prevWRA = whiteRookAMoved; u.prevWRH = whiteRookHMoved;
        u.prevBRA = blackRookAMoved; u.prevBRH = blackRookHMoved;
        u.prevHalfMoveClock = halfMoveClock;
        u.wasCastling = false;
        u.promotedTo = 0;

        int piece = u.movedPiece;
        boolean isPawn = pieceType(piece) == 1;
        boolean isCapture = u.capturedPiece != 0;

        // En passant capture
        if (isPawn && toC != fromC && u.capturedPiece == 0) {
            u.epR = fromR; u.epC = toC;
            u.epCapturedPiece = board[fromR][toC];
            board[fromR][toC] = 0;
            isCapture = true;
        }

        board[toR][toC] = piece;
        board[fromR][fromC] = 0;

        // Castling
        if (pieceType(piece) == 6 && Math.abs(toC - fromC) == 2) {
            u.wasCastling = true;
            if (toC == 6) {
                u.rookFromC = 7; u.rookToC = 5;
                board[fromR][5] = board[fromR][7]; board[fromR][7] = 0;
            } else {
                u.rookFromC = 0; u.rookToC = 3;
                board[fromR][3] = board[fromR][0]; board[fromR][0] = 0;
            }
        }

        // En passant rights
        enPassantCol = -1;
        if (isPawn && Math.abs(toR - fromR) == 2) enPassantCol = fromC;

        // Castling rights update
        if (piece == 6) whiteKingMoved = true;
        if (piece == -6) blackKingMoved = true;
        if (fromR == 7 && fromC == 0) whiteRookAMoved = true;
        if (fromR == 7 && fromC == 7) whiteRookHMoved = true;
        if (fromR == 0 && fromC == 0) blackRookAMoved = true;
        if (fromR == 0 && fromC == 7) blackRookHMoved = true;
        if (toR == 7 && toC == 0) whiteRookAMoved = true;
        if (toR == 7 && toC == 7) whiteRookHMoved = true;
        if (toR == 0 && toC == 0) blackRookAMoved = true;
        if (toR == 0 && toC == 7) blackRookHMoved = true;

        // Promotion
        if (isPawn && (toR == 0 || toR == 7)) {
            int promo = promoType > 0 ? promoType : 5; // always queen in search
            u.promotedTo = promo;
            board[toR][toC] = piece > 0 ? promo : -promo;
        }

        // Half-move clock
        if (isPawn || isCapture) halfMoveClock = 0;
        else halfMoveClock++;

        whiteTurn = !whiteTurn;
        return u;
    }

    private void unmakeSearchMove(UndoInfo u) {
        whiteTurn = !whiteTurn;
        halfMoveClock = u.prevHalfMoveClock;
        enPassantCol = u.prevEnPassantCol;
        whiteKingMoved = u.prevWKM; blackKingMoved = u.prevBKM;
        whiteRookAMoved = u.prevWRA; whiteRookHMoved = u.prevWRH;
        blackRookAMoved = u.prevBRA; blackRookHMoved = u.prevBRH;

        // Undo promotion
        board[u.fromR][u.fromC] = u.movedPiece;
        board[u.toR][u.toC] = u.capturedPiece;

        // Undo en passant capture
        if (u.epR >= 0) {
            board[u.epR][u.epC] = u.epCapturedPiece;
        }

        // Undo castling rook
        if (u.wasCastling) {
            board[u.fromR][u.rookFromC] = board[u.fromR][u.rookToC];
            board[u.fromR][u.rookToC] = 0;
        }
    }

    // MVV-LVA move ordering score
    private int moveOrderScore(int[] move) {
        int captured = board[move[2]][move[3]];
        int attacker = board[move[0]][move[1]];
        int score = 0;
        if (captured != 0) {
            score = PIECE_VALUES[pieceType(captured)] * 10 - PIECE_VALUES[pieceType(attacker)];
            score += 10000; // captures first
        }
        // Pawn promotion bonus
        if (pieceType(attacker) == 1 && (move[2] == 0 || move[2] == 7)) {
            score += 9000;
        }
        // Center control bonus for move ordering
        if ((move[2] == 3 || move[2] == 4) && (move[3] == 3 || move[3] == 4)) {
            score += 50;
        }
        return score;
    }

    private int negamax(int depth, int alpha, int beta, int ply, boolean addNoise) {
        // Check for draws
        if (halfMoveClock >= 100) return 0;
        if (positionHistory.size() > 4) {
            long h = computeHash();
            int count = 0;
            for (long ph : positionHistory) if (ph == h) count++;
            if (count >= 2) return 0; // treat as draw if approaching threefold
        }

        boolean inCheck = isInCheck(whiteTurn);

        if (depth <= 0) {
            if (inCheck) {
                // Search extension: don't stop in check
                depth = 1;
            } else {
                return quiescence(alpha, beta, 0, addNoise);
            }
        }

        List<int[]> moves = getAllLegalMoves(whiteTurn);

        if (moves.isEmpty()) {
            if (inCheck) return -20000 + ply; // checkmate — prefer faster mates
            return 0; // stalemate
        }

        // Sort moves by MVV-LVA
        moves.sort((a, b) -> moveOrderScore(b) - moveOrderScore(a));

        for (int[] move : moves) {
            UndoInfo undo = makeSearchMove(move[0], move[1], move[2], move[3], 0);
            int score = -negamax(depth - 1, -beta, -alpha, ply + 1, addNoise);
            unmakeSearchMove(undo);

            if (score >= beta) return beta;
            if (score > alpha) alpha = score;
        }
        return alpha;
    }

    // Quiescence search — only evaluate captures to avoid horizon effect
    private int quiescence(int alpha, int beta, int qDepth, boolean addNoise) {
        int standPat = evaluateBoard();
        if (!whiteTurn) standPat = -standPat;

        // Add noise at leaf for balanced bot
        if (addNoise) {
            standPat += random.nextInt(NOISE_RANGE * 2 + 1) - NOISE_RANGE;
        }

        if (qDepth >= 4) return standPat; // limit quiescence depth

        if (standPat >= beta) return beta;
        if (standPat > alpha) alpha = standPat;

        // Generate capture moves only
        List<int[]> captures = new ArrayList<>();
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (board[r][c] != 0 && isWhitePiece(board[r][c]) == whiteTurn)
                    for (int[] m : getLegalMoves(r, c)) {
                        boolean isCapture = board[m[0]][m[1]] != 0;
                        // Also count en passant and promotions as "loud" moves
                        boolean isPawnPromo = pieceType(board[r][c]) == 1 && (m[0] == 0 || m[0] == 7);
                        boolean isEP = pieceType(board[r][c]) == 1 && m[1] != c && board[m[0]][m[1]] == 0;
                        if (isCapture || isPawnPromo || isEP)
                            captures.add(new int[]{r, c, m[0], m[1]});
                    }

        captures.sort((a, b) -> moveOrderScore(b) - moveOrderScore(a));

        for (int[] move : captures) {
            UndoInfo undo = makeSearchMove(move[0], move[1], move[2], move[3], 0);
            int score = -quiescence(-beta, -alpha, qDepth + 1, addNoise);
            unmakeSearchMove(undo);

            if (score >= beta) return beta;
            if (score > alpha) alpha = score;
        }
        return alpha;
    }

    // =====================================================================
    //  BOT MOVE — balanced with probabilistic selection
    // =====================================================================
    private void botMove() {
        if (gameOver) { botThinking = false; return; }

        List<int[]> allMoves = getAllLegalMoves(false); // black moves
        if (allMoves.isEmpty()) { botThinking = false; return; }

        // Score all root moves
        List<int[]> scoredMoves = new ArrayList<>(); // [fromR, fromC, toR, toC, score]
        for (int[] move : allMoves) {
            UndoInfo undo = makeSearchMove(move[0], move[1], move[2], move[3], 0);
            int score = -negamax(BOT_DEPTH - 1, -100000, 100000, 1, true);
            unmakeSearchMove(undo);
            scoredMoves.add(new int[]{move[0], move[1], move[2], move[3], score});
        }

        // Sort by score descending (best first)
        scoredMoves.sort((a, b) -> b[4] - a[4]);

        // Probabilistic selection for balanced play
        int[] chosen;
        int roll = random.nextInt(100);
        if (scoredMoves.size() == 1) {
            chosen = scoredMoves.get(0);
        } else if (roll < BEST_MOVE_PERCENT) {
            // Pick best move (or one of the top moves within 15cp of best)
            int bestScore = scoredMoves.get(0)[4];
            List<int[]> topMoves = new ArrayList<>();
            for (int[] m : scoredMoves) {
                if (bestScore - m[4] <= 15) topMoves.add(m);
                else break;
            }
            chosen = topMoves.get(random.nextInt(topMoves.size()));
        } else if (roll < BEST_MOVE_PERCENT + SECOND_MOVE_PERCENT && scoredMoves.size() >= 2) {
            // Pick second-tier move (rank 2-4)
            int end = Math.min(4, scoredMoves.size());
            int start = Math.min(1, scoredMoves.size() - 1);
            chosen = scoredMoves.get(start + random.nextInt(end - start));
        } else {
            // Pick a random move from the top half
            int end = Math.max(1, scoredMoves.size() / 2);
            chosen = scoredMoves.get(random.nextInt(end));
        }

        // Check if it's a promotion
        int promoPiece = 0;
        if (pieceType(board[chosen[0]][chosen[1]]) == 1 && chosen[2] == 7) {
            // Bot always promotes to queen (smart bots do this 99% of the time)
            // But occasionally underpromote for fun (3% chance to knight)
            promoPiece = random.nextInt(100) < 3 ? 2 : 5;
        }

        botThinking = false;
        executeMove(chosen[0], chosen[1], chosen[2], chosen[3], promoPiece);
    }

    // =====================================================================
    //  UI UPDATES
    // =====================================================================
    private void updateStatus() {
        if (!gameOver) {
            if (botThinking) {
                txtStatus.setText("Roboter denkt...");
            } else {
                txtStatus.setText(whiteTurn ? "Weiß ist dran" : (vsBot ? "Roboter denkt..." : "Schwarz ist dran"));
            }
        }
        // Material count display
        int wMat = 0, bMat = 0;
        int[] v = {0,1,3,3,5,9,0};
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                int p = board[r][c];
                if (p > 0) wMat += v[p > 6 ? 0 : p];
                if (p < 0) bMat += v[-p > 6 ? 0 : -p];
            }
        txtInfo.setText("Weiß: " + wMat + " | Schwarz: " + bMat);
    }

    private void vibrate(int ms) {
        if (vibrator != null && vibrator.hasVibrator())
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() { super.onResume(); hideSystemUI(); }

    // =====================================================================
    //  BOARD VIEW (identical appearance)
    // =====================================================================
    private class BoardView extends View {
        private Paint lightPaint, darkPaint, selectedHL, moveHL, checkHL, lastMoveHL, textPaint, copyPaint;
        private float cellSize, offsetX, offsetY;

        public BoardView(Context ctx) {
            super(ctx);
            lightPaint = mp(0xFFF0D9B5); darkPaint = mp(0xFFB58863);
            selectedHL = mp(0x88FFEB3B); moveHL = mp(0x8866BB6A);
            checkHL = mp(0x88EF5350); lastMoveHL = mp(0x4400BCD4);
            textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            copyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            copyPaint.setColor(0x88888888);
            copyPaint.setTextSize(12f);
            copyPaint.setTextAlign(Paint.Align.RIGHT);
        }

        private Paint mp(int c) { Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); p.setColor(c); p.setStyle(Paint.Style.FILL); return p; }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth(), h = getHeight();
            cellSize = Math.min(w, h) / 8f;
            offsetX = (w - cellSize * 8) / 2f;
            offsetY = (h - cellSize * 8) / 2f;
            textPaint.setTextSize(cellSize * 0.75f);

            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    float left = offsetX + c * cellSize, top = offsetY + r * cellSize;
                    canvas.drawRect(left, top, left+cellSize, top+cellSize, (r+c)%2==0 ? lightPaint : darkPaint);

                    // Last move highlight
                    if ((r == lastMoveFromR && c == lastMoveFromC) || (r == lastMoveToR && c == lastMoveToC))
                        canvas.drawRect(left, top, left+cellSize, top+cellSize, lastMoveHL);
                    if (r == selectedRow && c == selectedCol)
                        canvas.drawRect(left, top, left+cellSize, top+cellSize, selectedHL);
                    for (int[] m : validMoves)
                        if (m[0]==r && m[1]==c)
                            canvas.drawRect(left, top, left+cellSize, top+cellSize, moveHL);
                    // Check highlight on king
                    int p = board[r][c];
                    if (p != 0 && pieceType(p) == 6 && isInCheck(p > 0))
                        canvas.drawRect(left, top, left+cellSize, top+cellSize, checkHL);

                    if (p != 0) {
                        String sym;
                        if (p > 0) sym = p <= 6 ? WHITE_PIECES[p] : WHITE_PIECES[5];
                        else sym = -p <= 6 ? BLACK_PIECES[-p] : BLACK_PIECES[5];
                        textPaint.setColor(p > 0 ? 0xFFFFFFFF : 0xFF1A1A1A);
                        textPaint.setShadowLayer(3f, 1f, 1f, p > 0 ? 0x88000000 : 0x88FFFFFF);
                        float cx = left + cellSize/2f;
                        float cy = top + cellSize/2f - (textPaint.descent()+textPaint.ascent())/2f;
                        canvas.drawText(sym, cx, cy, textPaint);
                        textPaint.clearShadowLayer();
                    }
                }
            }
            // Copyright
            canvas.drawText("© Moritzsoft", w - 8, h - 4, copyPaint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                int col = (int)((event.getX()-offsetX)/cellSize);
                int row = (int)((event.getY()-offsetY)/cellSize);
                if (row >= 0 && row < 8 && col >= 0 && col < 8) onCellTapped(row, col);
                return true;
            }
            return super.onTouchEvent(event);
        }
    }
}
