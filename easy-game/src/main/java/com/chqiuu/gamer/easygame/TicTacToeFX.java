package com.chqiuu.gamer.easygame;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class TicTacToeFX extends Application {

    private static final int BOARD_SIZE = 3; // 棋盘大小为 3x3

    private char[][] board = new char[BOARD_SIZE][BOARD_SIZE]; // 内部逻辑棋盘, ' ' 表示空, 'X', 'O'
    private char currentPlayer = 'X'; // 当前玩家，X 先手
    private boolean gameOver = false; // 游戏是否结束标志

    // --- UI 元素 ---
    private Button[][] cellButtons = new Button[BOARD_SIZE][BOARD_SIZE]; // 棋盘按钮数组
    private Label statusLabel; // 显示游戏状态（轮到谁、获胜者、平局）的标签
    private Button newGameButton; // 新游戏按钮

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("井字棋游戏 (中文版)"); // 设置窗口标题

        BorderPane root = new BorderPane(); // 使用 BorderPane 作为根布局

        // --- 顶部状态显示区域 ---
        VBox topPane = createTopPane();
        root.setTop(topPane);
        BorderPane.setAlignment(topPane, Pos.CENTER);
        BorderPane.setMargin(topPane, new Insets(10));

        // --- 中间棋盘区域 ---
        GridPane boardPane = createBoardPane();
        root.setCenter(boardPane);
        BorderPane.setAlignment(boardPane, Pos.CENTER);

        // --- 底部新游戏按钮区域 ---
        VBox bottomPane = createBottomPane();
        root.setBottom(bottomPane);
        BorderPane.setAlignment(bottomPane, Pos.CENTER);
        BorderPane.setMargin(bottomPane, new Insets(10));

        initializeGame(); // 初始化游戏状态和棋盘

        Scene scene = new Scene(root, 350, 450); // 设置场景大小
        primaryStage.setScene(scene);
        primaryStage.setResizable(false); // 禁止调整大小
        primaryStage.show(); // 显示窗口
    }

    // --- UI 创建辅助方法 ---

    /**
     * 创建顶部的状态显示面板
     * @return 包含状态标签的 VBox
     */
    private VBox createTopPane() {
        VBox vbox = new VBox();
        vbox.setAlignment(Pos.CENTER);
        statusLabel = new Label(); // 初始化标签，文本在 updateStatusLabel 中设置
        statusLabel.setFont(Font.font("Arial", 18)); // 设置字体
        vbox.getChildren().add(statusLabel);
        return vbox;
    }

    /**
     * 创建中间的 3x3 棋盘面板
     * @return 包含按钮的 GridPane
     */
    private GridPane createBoardPane() {
        GridPane gridPane = new GridPane();
        gridPane.setAlignment(Pos.CENTER);
        gridPane.setHgap(10); // 设置单元格水平间距
        gridPane.setVgap(10); // 设置单元格垂直间距
        gridPane.setPadding(new Insets(10)); // 设置内边距

        // 创建 3x3 的按钮并添加到 GridPane
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Button button = new Button();
                button.setMinSize(80, 80); // 设置按钮最小尺寸
                button.setFont(Font.font("Arial", FontWeight.BOLD, 32)); // 设置按钮字体
                final int r = row; // final 变量用于 lambda 表达式
                final int c = col;
                // 为每个按钮设置点击事件处理器
                button.setOnAction(event -> handleCellClick(r, c));
                cellButtons[row][col] = button; // 将按钮存入数组
                gridPane.add(button, col, row); // 添加到 GridPane (注意列在前，行在后)
            }
        }
        return gridPane;
    }

    /**
     * 创建底部的新游戏按钮面板
     * @return 包含新游戏按钮的 VBox
     */
    private VBox createBottomPane() {
        VBox vbox = new VBox();
        vbox.setAlignment(Pos.CENTER);
        newGameButton = new Button("开始新游戏");
        newGameButton.setFont(Font.font(16));
        newGameButton.setOnAction(event -> initializeGame()); // 点击按钮时初始化游戏
        vbox.getChildren().add(newGameButton);
        return vbox;
    }

    // --- 游戏逻辑方法 ---

    /**
     * 初始化或重置游戏状态和棋盘界面
     */
    private void initializeGame() {
        // 重置内部逻辑棋盘
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                board[row][col] = ' '; // ' ' 代表空格子
                // 重置界面按钮
                cellButtons[row][col].setText(""); // 清空按钮文本
                cellButtons[row][col].setDisable(false); // 启用按钮
                cellButtons[row][col].setStyle(""); // 清除可能存在的获胜样式
            }
        }
        currentPlayer = 'X'; // X 先手
        gameOver = false; // 游戏未结束
        updateStatusLabel(); // 更新状态标签
    }

    /**
     * 处理棋盘单元格按钮的点击事件
     * @param row 被点击按钮的行号 (0-2)
     * @param col 被点击按钮的列号 (0-2)
     */
    private void handleCellClick(int row, int col) {
        // 检查游戏是否已结束，或者当前格子是否已被占用
        if (gameOver || board[row][col] != ' ') {
            return; // 如果是，则不执行任何操作
        }

        // 更新内部逻辑棋盘
        board[row][col] = currentPlayer;

        // 更新被点击的按钮界面
        Button clickedButton = cellButtons[row][col];
        clickedButton.setText(String.valueOf(currentPlayer)); // 显示 X 或 O
        clickedButton.setDisable(true); // 禁用已点击的按钮

        // 检查当前玩家是否获胜
        if (checkWin(currentPlayer)) {
            gameOver = true;
            updateStatusLabel();
            // (可选) 高亮获胜的连线 - 稍微复杂，这里先用弹窗提示
            showResultAlert(currentPlayer + " 赢了!");
        }
        // 检查是否平局
        else if (checkDraw()) {
            gameOver = true;
            updateStatusLabel();
            showResultAlert("平局!");
        }
        // 如果游戏未结束，切换玩家
        else {
            switchPlayer();
            updateStatusLabel();
        }
    }

    /**
     * 切换当前玩家 (X -> O, O -> X)
     */
    private void switchPlayer() {
        currentPlayer = (currentPlayer == 'X') ? 'O' : 'X';
    }

    /**
     * 检查指定玩家是否获胜
     * @param player 要检查的玩家 ('X' 或 'O')
     * @return 如果该玩家获胜，返回 true，否则返回 false
     */
    private boolean checkWin(char player) {
        // 检查所有行
        for (int row = 0; row < BOARD_SIZE; row++) {
            if (board[row][0] == player && board[row][1] == player && board[row][2] == player) {
                // (可选) 高亮获胜行
                // highlightWin(row, 0, row, 1, row, 2);
                return true;
            }
        }
        // 检查所有列
        for (int col = 0; col < BOARD_SIZE; col++) {
            if (board[0][col] == player && board[1][col] == player && board[2][col] == player) {
                // (可选) 高亮获胜列
                // highlightWin(0, col, 1, col, 2, col);
                return true;
            }
        }
        // 检查对角线
        if (board[0][0] == player && board[1][1] == player && board[2][2] == player) {
            // (可选) 高亮主对角线
            // highlightWin(0, 0, 1, 1, 2, 2);
            return true;
        }
        if (board[0][2] == player && board[1][1] == player && board[2][0] == player) {
            // (可选) 高亮副对角线
            // highlightWin(0, 2, 1, 1, 2, 0);
            return true;
        }
        return false; // 没有获胜情况
    }

    /**
     * 检查游戏是否为平局
     * @return 如果棋盘已满且无人获胜，返回 true，否则返回 false
     */
    private boolean checkDraw() {
        // 如果棋盘上还有空格子，则不可能是平局
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (board[row][col] == ' ') {
                    return false;
                }
            }
        }
        // 如果棋盘已满，并且走到这里说明没有人获胜 (因为 checkWin 先被调用了)
        return true;
    }

    /**
     * 更新状态标签的文本内容
     */
    private void updateStatusLabel() {
        if (gameOver) {
            if (checkWin('X')) {
                statusLabel.setText("游戏结束 - X 获胜!");
            } else if (checkWin('O')) {
                statusLabel.setText("游戏结束 - O 获胜!");
            } else if (checkDraw()) {
                statusLabel.setText("游戏结束 - 平局!");
            }
        } else {
            statusLabel.setText("轮到 " + currentPlayer + " 下棋"); // 提示当前轮到谁
        }
    }

    /**
     * (可选) 高亮显示获胜的三个格子
     * @param r1 第一个格子的行
     * @param c1 第一个格子的列
     * @param r2 第二个格子的行
     * @param c2 第二个格子的列
     * @param r3 第三个格子的行
     * @param c3 第三个格子的列
     */
    private void highlightWin(int r1, int c1, int r2, int c2, int r3, int c3) {
        String winStyle = "-fx-background-color: lightgreen;"; // 设置获胜格子的背景色
        cellButtons[r1][c1].setStyle(winStyle);
        cellButtons[r2][c2].setStyle(winStyle);
        cellButtons[r3][c3].setStyle(winStyle);
    }

    /**
     * 显示一个包含游戏结果的提示框
     * @param message 要显示的消息
     */
    private void showResultAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION); // 创建信息类型的提示框
        alert.setTitle("游戏结果"); // 设置提示框标题
        alert.setHeaderText(null); // 不显示头部文本
        alert.setContentText(message); // 设置提示内容
        alert.showAndWait(); // 显示并等待用户关闭
    }

    // --- 主方法 ---
    public static void main(String[] args) {
        launch(args); // 启动 JavaFX 应用
    }
}