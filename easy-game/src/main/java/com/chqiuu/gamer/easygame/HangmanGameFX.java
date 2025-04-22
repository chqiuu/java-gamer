package com.chqiuu.gamer.easygame;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.*;

public class HangmanGameFX extends Application {

    // --- 常量定义 ---
    private static final int MAX_ERRORS = 6; // 最大允许错误次数 (对应小人绘制的6个部分)
    private static final double CANVAS_WIDTH = 300;
    private static final double CANVAS_HEIGHT = 300;
    private static final Color HANGMAN_COLOR = Color.BLACK;
    private static final double LINE_WIDTH = 3.0;

    // --- 单词库 ---
    private Map<String, List<String>> wordCategories = new HashMap<>();
    private String currentCategory = "Animals"; // 默认分类

    // --- 游戏状态变量 ---
    private String secretWord; // 要猜的秘密单词 (大写)
    private StringBuilder displayedWord; // 显示给玩家的单词 (带下划线)
    private int errors; // 当前错误次数
    private Set<Character> guessedLetters; // 已猜过的所有字母 (大写)
    private boolean gameOver; // 游戏是否结束

    // --- UI 元素 ---
    private Label categoryLabel;
    private ComboBox<String> categoryComboBox;
    private Label statusLabel; // 显示游戏提示信息
    private Label wordLabel; // 显示带下划线的单词
    private Label errorsLabel; // 显示错误次数/剩余机会
    private Label wrongGuessesLabel; // 显示猜错的字母
    private Canvas hangmanCanvas; // 绘制绞刑架和小人的画布
    private GraphicsContext gc; // 画布的绘图上下文
    private TilePane keyboardPane; // 放置字母按钮的面板
    private Button newGameButton;
    private Map<Character, Button> keyboardButtons = new HashMap<>(); // 存储字母按钮引用

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("刽子手游戏 (Hangman 英文单词版)");

        initializeWordCategories(); // 初始化单词库

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));

        // --- 顶部区域: 分类选择 和 状态信息 ---
        VBox topPane = createTopPane();
        root.setTop(topPane);
        BorderPane.setMargin(topPane, new Insets(0, 0, 15, 0));

        // --- 左侧区域: 绞刑架绘制 ---
        VBox leftPane = createHangmanPane();
        root.setLeft(leftPane);
        BorderPane.setMargin(leftPane, new Insets(0, 15, 0, 0));

        // --- 中间区域: 单词显示、错误信息、新游戏按钮 ---
        VBox centerPane = createCenterPane();
        root.setCenter(centerPane);

        // --- 底部区域: 虚拟键盘 ---
        keyboardPane = createKeyboardPane();
        root.setBottom(keyboardPane);
        BorderPane.setAlignment(keyboardPane, Pos.CENTER);
        BorderPane.setMargin(keyboardPane, new Insets(15, 0, 0, 0));


        initializeGame(); // 初始化第一局游戏

        Scene scene = new Scene(root, 750, 600); // 调整窗口大小
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    // --- 初始化单词分类 ---
    private void initializeWordCategories() {
        wordCategories.put("Animals", Arrays.asList("TIGER", "PANDA", "ELEPHANT", "GIRAFFE", "MONKEY", "KANGAROO", "PENGUIN", "SNAKE", "LION"));
        wordCategories.put("Fruits", Arrays.asList("APPLE", "BANANA", "STRAWBERRY", "GRAPE", "WATERMELON", "ORANGE", "MANGO", "PEACH"));
        wordCategories.put("Countries", Arrays.asList("CHINA", "AMERICA", "JAPAN", "FRANCE", "GERMANY", "BRAZIL", "INDIA", "CANADA", "RUSSIA"));
        // 可以根据需要添加更多分类和单词
    }

    // --- UI 创建辅助方法 ---

    private VBox createTopPane() {
        VBox vbox = new VBox(10);
        vbox.setAlignment(Pos.CENTER);

        HBox categorySelection = new HBox(10);
        categorySelection.setAlignment(Pos.CENTER);
        categoryLabel = new Label("选择单词分类:");
        categoryComboBox = new ComboBox<>();
        categoryComboBox.getItems().addAll(wordCategories.keySet());
        categoryComboBox.setValue(currentCategory);
        // 当选择新分类时，自动开始新游戏
        categoryComboBox.setOnAction(e -> {
            currentCategory = categoryComboBox.getValue();
            initializeGame();
        });
        categorySelection.getChildren().addAll(categoryLabel, categoryComboBox);

        statusLabel = new Label("请点击字母开始猜词");
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        statusLabel.setTextFill(Color.DARKBLUE);

        vbox.getChildren().addAll(categorySelection, statusLabel);
        return vbox;
    }

    private VBox createHangmanPane() {
        VBox vbox = new VBox(5);
        vbox.setAlignment(Pos.CENTER);
        Label canvasTitle = new Label("绞刑架");
        hangmanCanvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        gc = hangmanCanvas.getGraphicsContext2D();
        gc.setLineWidth(LINE_WIDTH);
        gc.setStroke(HANGMAN_COLOR);
        vbox.getChildren().addAll(canvasTitle, hangmanCanvas);
        return vbox;
    }

    private VBox createCenterPane() {
        VBox vbox = new VBox(15);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(10));

        wordLabel = new Label(); // 单词显示区域
        wordLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, 36)); // 使用等宽字体

        errorsLabel = new Label(); // 错误次数/剩余机会
        errorsLabel.setFont(Font.font(14));

        wrongGuessesLabel = new Label("猜错的字母: "); // 显示猜错的字母
        wrongGuessesLabel.setFont(Font.font(14));
        wrongGuessesLabel.setWrapText(true); // 允许换行

        newGameButton = new Button("开始新游戏");
        newGameButton.setFont(Font.font(16));
        newGameButton.setOnAction(e -> initializeGame());

        vbox.getChildren().addAll(wordLabel, errorsLabel, wrongGuessesLabel, newGameButton);
        return vbox;
    }

    private TilePane createKeyboardPane() {
        TilePane tilePane = new TilePane();
        tilePane.setPadding(new Insets(10));
        tilePane.setPrefColumns(7); // 设置键盘每行大约显示7个字母
        tilePane.setHgap(5);
        tilePane.setVgap(5);
        tilePane.setAlignment(Pos.CENTER);

        // 创建 A-Z 的按钮
        for (char c = 'A'; c <= 'Z'; c++) {
            Button button = new Button(String.valueOf(c));
            button.setFont(Font.font(16));
            button.setMinWidth(40); // 设置按钮最小宽度
            final char letter = c; // 用于 lambda 表达式
            button.setOnAction(e -> handleGuess(letter));
            keyboardButtons.put(c, button); // 存储按钮引用
            tilePane.getChildren().add(button);
        }
        return tilePane;
    }


    // --- 游戏逻辑方法 ---

    /**
     * 初始化或重置游戏
     */
    private void initializeGame() {
        errors = 0;
        guessedLetters = new HashSet<>();
        gameOver = false;

        // 选择新单词
        chooseNewWord();

        // 初始化显示的单词 (下划线)
        displayedWord = new StringBuilder();
        for (int i = 0; i < secretWord.length(); i++) {
            // 处理多音字或特殊情况，这里简单处理，认为一个汉字占一个下划线位
            if (Character.isLetter(secretWord.charAt(i))) { // 假设单词库都是字母或汉字
                displayedWord.append("_ "); // 用下划线和空格代替
            } else if (Character.toString(secretWord.charAt(i)).matches("\\p{IsHan}")) { // 判断是否汉字
                displayedWord.append("_ ");
            } else {
                displayedWord.append(secretWord.charAt(i)).append(" "); // 其他字符（如空格、标点）直接显示
            }
        }
        // 移除末尾多余的空格
        if(displayedWord.length() > 0) displayedWord.setLength(displayedWord.length()-1);


        // --- 重置 UI ---
        statusLabel.setText("游戏开始！猜猜这个 " + currentCategory + " 词");
        statusLabel.setTextFill(Color.DARKBLUE);
        wordLabel.setText(displayedWord.toString());
        errorsLabel.setText("错误次数: 0 / " + MAX_ERRORS);
        wrongGuessesLabel.setText("猜错的字母: ");
        clearCanvas(); // 清空画布
        drawGallows(); // 绘制基础绞刑架
        resetKeyboard(); // 启用所有键盘按钮
        categoryComboBox.setDisable(false); // 允许更改分类
    }

    /**
     * 从当前分类中随机选择一个新单词
     */
    private void chooseNewWord() {
        List<String> wordList = wordCategories.get(currentCategory);
        if (wordList == null || wordList.isEmpty()) {
            // 如果分类无效或为空，提供一个默认词
            secretWord = "默认单词"; // 或者可以抛出异常或提示用户
            statusLabel.setText("错误：找不到分类 '" + currentCategory + "' 的单词!");
            statusLabel.setTextFill(Color.RED);
        } else {
            Random random = new Random();
            secretWord = wordList.get(random.nextInt(wordList.size())).toUpperCase(); // 转换为大写处理
            // System.out.println("秘密单词: " + secretWord); // 调试用
        }
    }

    /**
     * 处理玩家点击字母按钮的事件
     * @param letter 玩家猜的字母 (大写)
     */
    private void handleGuess(char letter) {
        if (gameOver) return; // 游戏已结束，不处理

        letter = Character.toUpperCase(letter); // 确保是大写

        // 检查字母是否已经猜过
        if (guessedLetters.contains(letter)) {
            statusLabel.setText("你已经猜过字母 '" + letter + "' 了");
            statusLabel.setTextFill(Color.ORANGE);
            return;
        }

        guessedLetters.add(letter); // 将字母加入已猜集合
        keyboardButtons.get(letter).setDisable(true); // 禁用对应键盘按钮

        boolean found = false; // 标记本次猜测是否正确
        StringBuilder newDisplayedWord = new StringBuilder();
        String currentWordString = displayedWord.toString().replace(" ", ""); // 获取当前显示的不带空格的版本

        // 遍历秘密单词，检查猜测的字母是否存在
        for (int i = 0; i < secretWord.length(); i++) {
            if (secretWord.charAt(i) == letter) {
                // 如果找到匹配的字母，更新显示的单词
                // 注意：这里需要正确处理空格，我们按索引更新
                int displayIndex = i * 2; // 因为每个字符后有空格，所以索引乘以2
                if (displayIndex < displayedWord.length()) {
                    displayedWord.setCharAt(displayIndex, letter);
                }
                found = true;
            }
        }


        // 更新界面上的单词显示
        wordLabel.setText(displayedWord.toString());

        // 处理猜测结果
        if (found) {
            statusLabel.setText("猜对了！字母 '" + letter + "' 在单词中！");
            statusLabel.setTextFill(Color.GREEN);
            // 检查是否获胜
            if (checkWin()) {
                endGame(true);
            }
        } else {
            // 猜错了
            errors++;
            errorsLabel.setText("错误次数: " + errors + " / " + MAX_ERRORS);
            drawHangmanPart(errors); // 绘制小人的一部分
            updateWrongGuessesLabel(letter); // 更新猜错字母列表
            statusLabel.setText("猜错了！单词中没有字母 '" + letter + "'");
            statusLabel.setTextFill(Color.RED);
            // 检查是否失败
            if (checkLoss()) {
                endGame(false);
            }
        }
    }

    /**
     * 更新显示猜错字母的标签
     * @param wrongLetter 刚刚猜错的字母
     */
    private void updateWrongGuessesLabel(char wrongLetter) {
        String currentText = wrongGuessesLabel.getText();
        if (currentText.equals("猜错的字母: ")) {
            wrongGuessesLabel.setText(currentText + wrongLetter);
        } else {
            wrongGuessesLabel.setText(currentText + ", " + wrongLetter);
        }
    }


    /**
     * 检查玩家是否获胜 (所有字母都已猜出)
     * @return 如果获胜返回 true
     */
    private boolean checkWin() {
        // 如果显示的单词中不再包含下划线，则获胜
        return !displayedWord.toString().contains("_");
    }

    /**
     * 检查玩家是否失败 (错误次数达到上限)
     * @return 如果失败返回 true
     */
    private boolean checkLoss() {
        return errors >= MAX_ERRORS;
    }

    /**
     * 结束游戏的处理逻辑
     * @param won 玩家是否获胜
     */
    private void endGame(boolean won) {
        gameOver = true;
        disableKeyboard(); // 禁用所有键盘按钮
        categoryComboBox.setDisable(true); // 禁用分类选择

        if (won) {
            statusLabel.setText("恭喜你！你赢了！");
            statusLabel.setTextFill(Color.GREEN);
        } else {
            statusLabel.setText("很遗憾，你输了... 答案是: " + secretWord);
            statusLabel.setTextFill(Color.DARKRED);
            // 将答案显示出来，替换掉下划线
            StringBuilder finalWord = new StringBuilder();
            for (char c : secretWord.toCharArray()) {
                finalWord.append(c).append(" ");
            }
            if(finalWord.length() > 0) finalWord.setLength(finalWord.length()-1);
            wordLabel.setText(finalWord.toString());
            wordLabel.setTextFill(Color.DARKRED); // 用红色显示答案
        }
    }

    /**
     * 清空画布
     */
    private void clearCanvas() {
        gc.clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
    }

    /**
     * 绘制基础的绞刑架
     */
    private void drawGallows() {
        // 底座
        gc.strokeLine(CANVAS_WIDTH * 0.1, CANVAS_HEIGHT * 0.9, CANVAS_WIDTH * 0.7, CANVAS_HEIGHT * 0.9);
        // 垂直杆
        gc.strokeLine(CANVAS_WIDTH * 0.25, CANVAS_HEIGHT * 0.9, CANVAS_WIDTH * 0.25, CANVAS_HEIGHT * 0.1);
        // 水平杆
        gc.strokeLine(CANVAS_WIDTH * 0.25, CANVAS_HEIGHT * 0.1, CANVAS_WIDTH * 0.65, CANVAS_HEIGHT * 0.1);
        // 绳子短竖线
        gc.strokeLine(CANVAS_WIDTH * 0.65, CANVAS_HEIGHT * 0.1, CANVAS_WIDTH * 0.65, CANVAS_HEIGHT * 0.2);
    }

    /**
     * 根据错误次数绘制小人的一个部分
     * @param errorCount 当前错误次数 (1-6)
     */
    private void drawHangmanPart(int errorCount) {
        double headX = CANVAS_WIDTH * 0.65;
        double headY = CANVAS_HEIGHT * 0.2 + 25; // 头心 Y 坐标
        double headRadius = 25;
        double bodyStartY = headY + headRadius;
        double bodyEndY = bodyStartY + 70;
        double armY = bodyStartY + 25;
        double legY = bodyEndY;

        switch (errorCount) {
            case 1: // 头
                gc.strokeOval(headX - headRadius, headY - headRadius, headRadius * 2, headRadius * 2);
                break;
            case 2: // 身体
                gc.strokeLine(headX, bodyStartY, headX, bodyEndY);
                break;
            case 3: // 左臂
                gc.strokeLine(headX, armY, headX - 35, armY + 25);
                break;
            case 4: // 右臂
                gc.strokeLine(headX, armY, headX + 35, armY + 25);
                break;
            case 5: // 左腿
                gc.strokeLine(headX, legY, headX - 30, legY + 50);
                break;
            case 6: // 右腿
                gc.strokeLine(headX, legY, headX + 30, legY + 50);
                break;
        }
    }

    /**
     * 重置（启用）所有虚拟键盘按钮
     */
    private void resetKeyboard() {
        for (Button button : keyboardButtons.values()) {
            button.setDisable(false);
        }
    }

    /**
     * 禁用所有虚拟键盘按钮
     */
    private void disableKeyboard() {
        for (Button button : keyboardButtons.values()) {
            button.setDisable(true);
        }
    }

    // --- 主方法 ---
    public static void main(String[] args) {
        launch(args);
    }
}