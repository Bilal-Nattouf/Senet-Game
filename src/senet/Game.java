package senet;

import java.util.Scanner;

/**
 * الفئة الرئيسية للعبة - تتحكم في سير اللعبة
 */
public class Game {
    private State gameState;
    private Scanner scanner;

    public Game() {
        gameState = new State();
        scanner = new Scanner(System.in);
    }

    /**
     * بدء اللعبة
     */
    public void start() {
        showWelcome();
        setupGame();

        // اللعبة الرئيسية
        while (!gameState.isGameOver()) {
            gameState.printBoard();

            if (gameState.isWhiteTurn()) {
                computerTurn();
            } else {
                playerTurn();
            }
        }

        endGame();
    }

    /**
     * إعداد اللعبة
     */
    private void setupGame() {
        System.out.print("أدخل عمق البحث (3-5 موصى به): ");
        int depth = scanner.nextInt();
        gameState.setSearchDepth(depth);

        System.out.print("تفعيل وضع التصحيح؟ (نعم/لا): ");
        String debug = scanner.next();
        gameState.setDebugMode(debug.toLowerCase().contains("نعم"));

        System.out.println("\nابدأ اللعب!");
    }

    /**
     * دور الكمبيوتر
     */
    private void computerTurn() {
        System.out.println("\n🤖 دور الكمبيوتر (الأبيض)...");

        // الرمية
        int roll = gameState.rollDice();
        System.out.println("الرمية: " + roll + " خطوات");

        // البحث عن أفضل حركة
        int[] bestMove = gameState.findBestMove();

        if (bestMove != null) {
            System.out.printf("الكمبيوتر يحرك من %d إلى %s%n",
                    bestMove[0],
                    bestMove[1] == 0 ? "الخارج" : bestMove[1]);

            gameState.applyMove(bestMove[0], bestMove[1]);

            if (bestMove[1] == 0) {
                System.out.println("الكمبيوتر أخرج حجرًا!");
            }
        } else {
            System.out.println("لا توجد حركات ممكنة. يمر الدور.");
            gameState.applyMove(0, 0); // حركة وهمية لتمرير الدور
        }

        pause(1500);
    }

    /**
     * دور اللاعب البشري
     */
    private void playerTurn() {
        System.out.println("\n👤 دورك (الأسود)...");

        // الرمية
        int roll = gameState.rollDice();
        System.out.println("رميتك: " + roll + " خطوات");

        // عرض الحركات الممكنة
        System.out.println("الحركات الممكنة:");
        for (int[] move : gameState.getPossibleMoves()) {
            String to = (move[1] == 0) ? "الخارج" : String.valueOf(move[1]);
            System.out.printf("  %d → %s%n", move[0], to);
        }

        // استلام الحركة من المستخدم
        boolean validMove = false;
        while (!validMove) {
            System.out.print("أدخل رقم المربع المصدر (أو 0 للخروج من اللعبة): ");
            int from = scanner.nextInt();

            if (from == 0) {
                System.out.println("إنهاء اللعبة...");
                System.exit(0);
            }

            // التحقق من صحة الحركة
            for (int[] move : gameState.getPossibleMoves()) {
                if (move[0] == from) {
                    gameState.applyMove(move[0], move[1]);
                    validMove = true;

                    if (move[1] == 0) {
                        System.out.println("أخرجت حجرًا!");
                    }
                    break;
                }
            }

            if (!validMove) {
                System.out.println("حركة غير صحيحة. حاول مرة أخرى.");
            }
        }
    }

    /**
     * إنهاء اللعبة وعرض النتائج
     */
    private void endGame() {
        System.out.println("\n" + "★".repeat(50));
        System.out.println("            نهاية اللعبة!");
        System.out.println("★".repeat(50));

        gameState.printBoard();

        int winner = gameState.getWinner();
        if (winner == State.WHITE) {
            System.out.println("🎖️  الكمبيوتر فاز!");
        } else {
            System.out.println("🏆  أنت الفائز!");
        }

        System.out.println("\nإحصائيات البحث:");
        System.out.println("العقد المفتوحة: " + gameState.getNodesVisited());

        scanner.close();
    }

    /**
     * عرض رسالة الترحيب
     */
    private void showWelcome() {
        System.out.println("=".repeat(60));
        System.out.println("           لعبة سيئت - مشروع الذكاء الصنعي");
        System.out.println("=".repeat(60));
        System.out.println("""
            
            القواعد:
            • لكل لاعب 7 أحجار (أبيض: كمبيوتر، أسود: أنت)
            • الرمية: 4 عصي (0=فاتح، 1=داكن)
            • المجموع 0 → 5 خطوات، 1-4 → نفس العدد
            • الهدف: إخراج جميع أحجارك أولاً
            
            المربعات الخاصة:
            15: بيت البعث    26: بيت السعادة
            27: بيت الماء   28: بيت الحقائق الثلاث
            29: بيت إعادة أنوم  30: بيت حورس
            """);
    }

    /**
     * تأخير مؤقت
     */
    private void pause(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            // تجاهل
        }
    }

    /**
     * الدالة الرئيسية
     */
    public static void main(String[] args) {
        Game game = new Game();
        game.start();
    }
}
