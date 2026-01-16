package senet;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * حالة اللعبة - تمثل الوضع الحالي للعبة
 * تحتوي على كل شيء: اللوحة، اللاعب الحالي، الأحجار الخارجة، إلخ.
 */
public class State implements Cloneable {
    // === ثوابت اللعبة ===
    public static final int BOARD_SIZE = 30;
    public static final int WHITE = 1;
    public static final int BLACK = 2;
    public static final int EMPTY = 0;

    // === خصائص اللعبة ===
    private int[] board;                    // مصفوفة اللوحة (1-30)
    private boolean whiteTurn;              // هل دور الأبيض؟
    private int lastDiceRoll;               // نتيجة آخر رمية
    private int whitePiecesOut;             // أحجار الأبيض الخارجة
    private int blackPiecesOut;             // أحجار الأسود الخارجة
    private Map<Integer, Integer> specialConditions; // حالات خاصة للأحجار

    // === أداة الرمي ===
    private java.util.Random random = new java.util.Random();

    // === احتمالات الرمية ===
    private static final double[] DICE_PROBABILITIES = {0.0625, 0.25, 0.375, 0.25, 0.0625};

    // === خوارزمية البحث ===
    private int searchDepth = 3;
    private boolean debugMode = false;
    private int nodesVisited = 0;

    // === مُنشئ ===
    public State() {
        board = new int[BOARD_SIZE + 1]; // الفهرس 0 غير مستخدم
        specialConditions = new HashMap<>();
        initializeBoard();
    }

    // ==================== قسم التهيئة ====================

    /**
     * تهيئة اللوحة بالوضع الابتدائي
     */
    private void initializeBoard() {
        // توزيع الأحجار بالتناوب في المربعات 1-14
        for (int i = 1; i <= 14; i++) {
            board[i] = (i % 2 == 1) ? WHITE : BLACK;
        }
        // بقية المربعات فارغة
        for (int i = 15; i <= BOARD_SIZE; i++) {
            board[i] = EMPTY;
        }

        whiteTurn = true;
        lastDiceRoll = 0;
        whitePiecesOut = 0;
        blackPiecesOut = 0;
    }

    // ==================== قسم العمليات الأساسية ====================

    /**
     * رمي العصي الأربع وإرجاع عدد الخطوات (1-5)
     */
    public int rollDice() {
        double rand = random.nextDouble();
        double cumulative = 0.0;

        for (int i = 0; i < DICE_PROBABILITIES.length; i++) {
            cumulative += DICE_PROBABILITIES[i];
            if (rand <= cumulative) {
                lastDiceRoll = (i == 0) ? 5 : i;
                return lastDiceRoll;
            }
        }
        lastDiceRoll = 1;
        return 1;
    }

    /**
     * الحصول على اللاعب الحالي
     */
    public int getCurrentPlayer() {
        return whiteTurn ? WHITE : BLACK;
    }

    /**
     * الحصول على اللاعب الخصم
     */
    public int getOpponent() {
        return whiteTurn ? BLACK : WHITE;
    }

    // ==================== قسم الحركات ====================

    /**
     * توليد جميع الحركات الممكنة
     */
    public List<int[]> getPossibleMoves() {
        List<int[]> moves = new ArrayList<>();
        int currentPlayer = getCurrentPlayer();

        for (int from = 1; from <= BOARD_SIZE; from++) {
            if (board[from] == currentPlayer && canMove(from, lastDiceRoll)) {
                int to = from + lastDiceRoll;
                if (to > BOARD_SIZE) {
                    to = 0; // يعني الخروج
                }
                moves.add(new int[]{from, to});
            }
        }

        return moves;
    }

    /**
     * التحقق من إمكانية الحركة
     */
    private boolean canMove(int from, int diceRoll) {
        // التحقق من القيود الخاصة
        Integer requiredRoll = specialConditions.get(from);
        if (requiredRoll != null) {
            return requiredRoll == -1 || requiredRoll == diceRoll;
        }

        int to = from + diceRoll;

        // لا يمكن القفز فوق بيت السعادة (26)
        if (from < 26 && to > 26) {
            return false;
        }

        // الخروج من اللوحة
        if (to > BOARD_SIZE) {
            return canExit(from, diceRoll);
        }

        // لا يمكن الهبوط على حجر نفس اللاعب
        return board[to] != getCurrentPlayer();
    }

    /**
     * التحقق من إمكانية الخروج
     */
    private boolean canExit(int from, int diceRoll) {
        return diceRoll >= (31 - from);
    }

    /**
     * تطبيق حركة على الحالة
     */
    public void applyMove(int from, int to) {
        int player = board[from];

        if (to == 0) { // خروج
            board[from] = EMPTY;
            if (player == WHITE) whitePiecesOut++;
            else blackPiecesOut++;
        } else {
            // إذا كان المربع الهدف مشغول بحجر الخصم
            if (board[to] != EMPTY && board[to] != player) {
                // تبادل المواقع
                int temp = board[to];
                board[to] = player;
                board[from] = temp;
            } else {
                board[to] = player;
                board[from] = EMPTY;
            }

            // التحقق من المربعات الخاصة
            checkSpecialSquare(to, player);
        }

        whiteTurn = !whiteTurn;
    }

    // ==================== قسم المربعات الخاصة ====================

    /**
     * تطبيق قواعد المربعات الخاصة
     */
    private void checkSpecialSquare(int square, int player) {
        switch (square) {
            case 15: // بيت البعث
                // لا شيء خاص
                break;

            case 27: // بيت الماء
                moveToRebirth(square, player);
                break;

            case 28: // بيت الحقائق الثلاث
                specialConditions.put(square, 3);
                break;

            case 29: // بيت إعادة أنوم
                specialConditions.put(square, 2);
                break;

            case 30: // بيت حورس
                specialConditions.put(square, -1);
                break;
        }
    }

    /**
     * نقل حجر إلى بيت البعث
     */
    private void moveToRebirth(int fromSquare, int player) {
        board[fromSquare] = EMPTY;
        for (int i = 15; i <= BOARD_SIZE; i++) {
            if (board[i] == EMPTY) {
                board[i] = player;
                break;
            }
        }
    }

    // ==================== قسم التقييم والبحث ====================

    /**
     * دالة التقييم (Heuristic)
     */
    public double evaluate() {
        double score = 0.0;

        // 1. الأحجار الخارجة (الأهم)
        score += (whitePiecesOut - blackPiecesOut) * 100;

        // 2. تقدم الأحجار
        for (int i = 1; i <= BOARD_SIZE; i++) {
            if (board[i] == WHITE) score += i;
            else if (board[i] == BLACK) score -= i;
        }

        // 3. السيطرة على المربعات الخاصة
        int[] specialSquares = {15, 26, 27, 28, 29, 30};
        for (int sq : specialSquares) {
            if (board[sq] == WHITE) score += 10;
            else if (board[sq] == BLACK) score -= 10;
        }

        // إذا كان دور الأسود، نعكس القيمة
        if (!whiteTurn) score = -score;

        return score;
    }

    /**
     * خوارزمية Expectiminimax المبسطة
     */
    public int[] findBestMove() {
        nodesVisited = 0;
        List<int[]> moves = getPossibleMoves();

        if (moves.isEmpty()) return null;

        double bestValue = Double.NEGATIVE_INFINITY;
        int[] bestMove = null;

        for (int[] move : moves) {
            State nextState = this.clone();
            nextState.applyMove(move[0], move[1]);

            double value = expectiminimax(nextState, searchDepth - 1, "CHANCE");

            if (debugMode) {
                System.out.printf("الحركة [%d→%d] القيمة: %.2f%n",
                        move[0], move[1], value);
            }

            if (value > bestValue) {
                bestValue = value;
                bestMove = move;
            }
        }

        if (debugMode) {
            System.out.println("العقد المفتوحة: " + nodesVisited);
        }

        return bestMove;
    }

    /**
     * الدالة العودية للخوارزمية
     */
    private double expectiminimax(State state, int depth, String nodeType) {
        nodesVisited++;

        if (depth == 0 || state.isGameOver()) {
            return state.evaluate();
        }

        switch (nodeType) {
            case "MAX":
                return maxValue(state, depth);
            case "MIN":
                return minValue(state, depth);
            case "CHANCE":
                return chanceValue(state, depth);
            default:
                return state.evaluate();
        }
    }

    /**
     * عقدة MAX (الكمبيوتر)
     */
    private double maxValue(State state, int depth) {
        double best = Double.NEGATIVE_INFINITY;
        List<int[]> moves = state.getPossibleMoves();

        for (int[] move : moves) {
            State next = state.clone();
            next.applyMove(move[0], move[1]);
            double value = expectiminimax(next, depth - 1, "CHANCE");
            best = Math.max(best, value);
        }

        return best;
    }

    /**
     * عقدة MIN (اللاعب البشري)
     */
    private double minValue(State state, int depth) {
        double best = Double.POSITIVE_INFINITY;
        List<int[]> moves = state.getPossibleMoves();

        for (int[] move : moves) {
            State next = state.clone();
            next.applyMove(move[0], move[1]);
            double value = expectiminimax(next, depth - 1, "CHANCE");
            best = Math.min(best, value);
        }

        return best;
    }

    /**
     * عقدة CHANCE (رمي العصي)
     */
    private double chanceValue(State state, int depth) {
        double expected = 0.0;

        // جميع النتائج الممكنة (1-5) مع احتمالاتها
        for (int i = 0; i < DICE_PROBABILITIES.length; i++) {
            int diceValue = (i == 0) ? 5 : i;
            double prob = DICE_PROBABILITIES[i];

            State next = state.clone();
            next.lastDiceRoll = diceValue;

            String nextNodeType = state.whiteTurn ? "MAX" : "MIN";
            double value = expectiminimax(next, depth, nextNodeType);
            expected += prob * value;
        }

        return expected;
    }

    // ==================== قسم التحقق من النهاية ====================

    /**
     * التحقق من انتهاء اللعبة
     */
    public boolean isGameOver() {
        return whitePiecesOut == 7 || blackPiecesOut == 7;
    }

    /**
     * الحصول على الفائز
     */
    public int getWinner() {
        if (whitePiecesOut == 7) return WHITE;
        if (blackPiecesOut == 7) return BLACK;
        return EMPTY;
    }

    // ==================== قسم عرض اللوحة ====================

    /**
     * طباعة اللوحة بشكل واضح
     */
    public void printBoard() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("            لوحة سيئت");
        System.out.println("=".repeat(50));

        // الصفوف الثلاثة
        printRow(1, 10, "→");
        printRow(20, 11, "←");
        printRow(21, 30, "→");

        System.out.println("\nمعلومات:");
        System.out.println("الدور: " + (whiteTurn ? "الأبيض" : "الأسود"));
        System.out.println("الرمية: " + lastDiceRoll);
        System.out.println("الخارج: ⚪" + whitePiecesOut + " ⚫" + blackPiecesOut);
        System.out.println("=".repeat(50));
    }

    /**
     * طباعة صف من اللوحة
     */
    private void printRow(int start, int end, String direction) {
        String rowName = "";
        if (start == 1) rowName = "الصف 1: ";
        else if (start == 20) rowName = "الصف 2: ";
        else rowName = "الصف 3: ";

        System.out.print(rowName);

        if (direction.equals("→")) {
            for (int i = start; i <= end; i++) {
                printSquare(i);
            }
        } else {
            for (int i = start; i >= end; i--) {
                printSquare(i);
            }
        }
        System.out.println();
    }

    /**
     * طباعة مربع واحد
     */
    private void printSquare(int square) {
        String symbol;
        switch (board[square]) {
            case WHITE: symbol = "W"; break;
            case BLACK: symbol = "B"; break;
            default: symbol = ".";
        }

        // تلوين المربعات الخاصة
        if (isSpecialSquare(square)) {
            System.out.print("[" + symbol + square + "] ");
        } else {
            System.out.print(" " + symbol + square + "  ");
        }
    }

    /**
     * التحقق إذا كان المربع خاصًا
     */
    private boolean isSpecialSquare(int square) {
        return square == 15 || square == 26 || square >= 27;
    }

    // ==================== قسم الإعدادات ====================

    /**
     * ضبط عمق البحث
     */
    public void setSearchDepth(int depth) {
        this.searchDepth = depth;
    }

    /**
     * تفعيل وضع التصحيح
     */
    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
    }

    /**
     * نسخ الحالة
     */
    @Override
    public State clone() {
        State clone = new State();
        System.arraycopy(this.board, 0, clone.board, 0, this.board.length);
        clone.whiteTurn = this.whiteTurn;
        clone.lastDiceRoll = this.lastDiceRoll;
        clone.whitePiecesOut = this.whitePiecesOut;
        clone.blackPiecesOut = this.blackPiecesOut;
        clone.specialConditions = new HashMap<>(this.specialConditions);
        clone.searchDepth = this.searchDepth;
        clone.debugMode = this.debugMode;
        return clone;
    }

    // ==================== قسم Getters ====================

    public int[] getBoard() { return board; }
    public boolean isWhiteTurn() { return whiteTurn; }
    public int getLastDiceRoll() { return lastDiceRoll; }
    public int getWhitePiecesOut() { return whitePiecesOut; }
    public int getBlackPiecesOut() { return blackPiecesOut; }
    public int getNodesVisited() { return nodesVisited; }
}

/*


    State :
            {
                blackOut , whiteOut,
                Array[SquareType]
                evaluation()
                getNextStates()

            }

    Game :
        coumputerMove() , playerMove() , miniMove() , maxMove() , chance()S

    Enum : SquareType :
                        { EMPTY, BLACK , WHITE }


 */