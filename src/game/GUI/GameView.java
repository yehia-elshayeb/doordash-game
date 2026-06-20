package game.GUI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import game.engine.Board;
import game.engine.Constants;
import game.engine.Game;
import game.engine.Role;
import game.engine.cards.Card;
import game.engine.cards.ConfusionCard;
import game.engine.cards.EnergyStealCard;
import game.engine.cards.ShieldCard;
import game.engine.cards.StartOverCard;
import game.engine.cards.SwapperCard;
import game.engine.cells.Cell;
import game.engine.cells.DoorCell;
import game.engine.exceptions.InvalidMoveException;
import game.engine.exceptions.OutOfEnergyException;
import game.engine.monsters.Dasher;
import game.engine.monsters.Monster;
import game.engine.monsters.MultiTasker;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

public class GameView {

    private final GameApp app;
    private final Game game;
    private final Random rng = new Random();
    private final Scene scene;

    private int turnNum = 1;
    private int lastRoll = 0;
    private Card lastCard = null;
    private final ArrayList<String> log = new ArrayList<String>();
    private boolean uiLocked = false;

    private final Label hTurn    = new Label();
    private final Label hCurrent = new Label();
    private final Label hDeck    = new Label();

    private final VBox playerBox   = new VBox(6);
    private final VBox opponentBox = new VBox(6);
    private final StackPane playerWrap   = new StackPane();
    private final StackPane opponentWrap = new StackPane();

    private final BoardCell[] cells = new BoardCell[Constants.BOARD_SIZE];
    private final GridPane boardGrid = new GridPane();

    private final Button powerupBtn = actionBtn("POWER-UP", Color.web("#2DA6BD"));
    private final Button rollBtn    = actionBtn("ROLL DICE", GameApp.CORAL);
    private final Button skipBtn    = actionBtn("SKIP TURN", Color.web("#5A6BAA"));

    private final Pane dice = new Pane();
    private final Circle[] pips = new Circle[9];

    private final VBox logBox = new VBox(2);
    private final Pane overlay = new Pane();

    public GameView(GameApp app, Role role) throws IOException {
        this.app = app;
        this.game = new Game(role);
        log.add("Battle begins: " + game.getPlayer().getName() + " ("
            + game.getPlayer().getRole() + ") vs " + game.getOpponent().getName() + " ("
            + game.getOpponent().getRole() + ").");

        BorderPane main = new BorderPane();
        main.setBackground(GameApp.bgGradient());
        main.setTop(buildHeader());
        main.setLeft(wrap(playerWrap, playerBox));
        main.setRight(wrap(opponentWrap, opponentBox));
        main.setCenter(buildBoard());
        main.setBottom(buildBottom());

        overlay.setMouseTransparent(true);
        overlay.setPickOnBounds(false);

        StackPane root = new StackPane(main, overlay);
        scene = new Scene(root, GameApp.W, GameApp.H);
        refresh();
    }

    public Scene getScene() { return scene; }

    private HBox buildHeader() {
        Label logo = new Label("DOORDASH");
        logo.setFont(Font.font("Georgia", FontWeight.BOLD, 22));
        logo.setTextFill(GameApp.GOLD);

        hTurn.setFont(Font.font("Verdana", FontWeight.BOLD, 14));
        hTurn.setTextFill(Color.WHITE);
        hCurrent.setFont(Font.font("Verdana", FontWeight.BOLD, 14));
        hCurrent.setTextFill(GameApp.ICE);
        hDeck.setFont(Font.font("Verdana", 13));
        hDeck.setTextFill(GameApp.ICE);

        Label exit = new Label("Press X to exit");
        exit.setFont(Font.font("Verdana", 11));
        exit.setTextFill(Color.web("#8AA0DD"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox box = new HBox(28, logo, hTurn, hCurrent, spacer, hDeck, exit);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(16, 26, 16, 26));
        LinearGradient lg = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#0A1230")), new Stop(1, Color.web("#1B2660")));
        box.setBackground(new Background(new BackgroundFill(lg, CornerRadii.EMPTY, Insets.EMPTY)));
        box.setMinHeight(68);
        return box;
    }

    private VBox wrap(StackPane wrap, VBox content) {
        content.setPadding(Insets.EMPTY);
        wrap.getChildren().add(content);
        wrap.setMinWidth(252);
        wrap.setMaxWidth(252);
        wrap.setPadding(new Insets(20, 14, 20, 14));
        return new VBox(wrap);
    }

    private StackPane buildBoard() {
        boardGrid.setHgap(0);
        boardGrid.setVgap(0);
        boardGrid.setAlignment(Pos.CENTER);
        for (int i = 0; i < Constants.BOARD_SIZE; i++) {
            BoardCell c = new BoardCell(i);
            cells[i] = c;
            int[] rc = indexToRowCol(i);
            int displayRow = Constants.BOARD_ROWS - 1 - rc[0];
            int displayCol = rc[1];
            boardGrid.add(c, displayCol, displayRow);
        }
        StackPane area = new StackPane(boardGrid);
        area.setPadding(new Insets(8, 16, 8, 16));
        return area;
    }

    private VBox buildBottom() {
        buildDice();
        powerupBtn.setOnAction(e -> onPowerup());
        rollBtn.setOnAction(e -> onRoll());
        skipBtn.setOnAction(e -> onSkip());

        HBox buttons = new HBox(14, powerupBtn, rollBtn, skipBtn);
        buttons.setAlignment(Pos.CENTER);

        HBox actionBar = new HBox(28, dice, buttons);
        actionBar.setAlignment(Pos.CENTER);
        actionBar.setPadding(new Insets(10, 24, 10, 24));

        Label logHeader = new Label("Game log");
        logHeader.setFont(Font.font("Verdana", FontWeight.BOLD, 11));
        logHeader.setTextFill(GameApp.GOLD);
        VBox.setMargin(logHeader, new Insets(0, 0, 0, 22));

        logBox.setPadding(new Insets(2, 18, 4, 18));

        VBox bottom = new VBox(2, actionBar, logHeader, logBox);
        bottom.setPadding(new Insets(2, 0, 8, 0));
        bottom.setMinHeight(168);
        bottom.setMaxHeight(168);

        LinearGradient lg = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#0F1B3D", 0.0)), new Stop(1, Color.web("#0A1230", 0.65)));
        bottom.setBackground(new Background(new BackgroundFill(lg, CornerRadii.EMPTY, Insets.EMPTY)));
        return bottom;
    }

    private void buildDice() {
        dice.setMinSize(78, 78);
        dice.setMaxSize(78, 78);
        LinearGradient g = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#FAFAFA")), new Stop(1, Color.web("#D8DBE8")));
        dice.setBackground(new Background(new BackgroundFill(g, new CornerRadii(12), Insets.EMPTY)));
        dice.setBorder(new Border(new BorderStroke(Color.web("#222"),
            BorderStrokeStyle.SOLID, new CornerRadii(12), new BorderWidths(1.5))));
        DropShadow ds = new DropShadow(12, Color.web("#000000", 0.55));
        ds.setOffsetY(3);
        dice.setEffect(ds);

        double[] xs = {16, 39, 62};
        double[] ys = {16, 39, 62};
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++) {
                Circle dot = new Circle(5.5, Color.web("#1A1F40"));
                dot.setCenterX(xs[c]);
                dot.setCenterY(ys[r]);
                dot.setOpacity(0);
                pips[r * 3 + c] = dot;
                dice.getChildren().add(dot);
            }
        setDice(0);
    }

    private void setDice(int n) {
        for (Circle p : pips) p.setOpacity(0);
        if (n <= 0 || n > 6) return;
        int[][] pattern = {
            {},
            {4},
            {0, 8},
            {0, 4, 8},
            {0, 2, 6, 8},
            {0, 2, 4, 6, 8},
            {0, 2, 3, 5, 6, 8}
        };
        for (int idx : pattern[n]) pips[idx].setOpacity(1);
    }

    private Button actionBtn(String text, Color color) {
        Button b = new Button(text);
        b.setFont(Font.font("Verdana", FontWeight.BOLD, 13));
        b.setMinHeight(48);
        b.setStyle(btnStyle(color, false));
        DropShadow ds = new DropShadow(10, color.deriveColor(0, 1, 1, 0.6));
        b.setEffect(ds);
        b.setOnMouseEntered(e -> { if (!b.isDisabled()) b.setStyle(btnStyle(color, true)); });
        b.setOnMouseExited(e ->  { if (!b.isDisabled()) b.setStyle(btnStyle(color, false)); });
        return b;
    }

    private String btnStyle(Color color, boolean hover) {
        Color c = hover ? color.brighter() : color;
        return "-fx-background-color: " + GameApp.toRgba(c) + ";"
            + "-fx-text-fill: white; -fx-padding: 10 22 10 22;"
            + "-fx-background-radius: 8; -fx-cursor: hand;";
    }

    private void populatePanel(VBox panel, Monster m, boolean isCurrent, boolean isPlayer) {
        panel.getChildren().clear();
        if (m == null) return;

        Color roleColor = m.getOriginalRole() == Role.SCARER ? GameApp.SCARER_C : GameApp.LAUGHER_C;
        Color frame = isCurrent ? GameApp.GOLD : Color.web("#3A4470");
        LinearGradient bg = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#2A3580")), new Stop(1, Color.web("#1A2453")));

        StackPane parent = (StackPane) panel.getParent();
        parent.setBackground(new Background(new BackgroundFill(bg, new CornerRadii(14), Insets.EMPTY)));
        parent.setBorder(new Border(new BorderStroke(frame, BorderStrokeStyle.SOLID,
            new CornerRadii(14), new BorderWidths(isCurrent ? 2.5 : 1.5))));
        if (isCurrent) {
            DropShadow glow = new DropShadow(22, GameApp.GOLD.deriveColor(0, 1, 1, 0.55));
            glow.setSpread(0.25);
            parent.setEffect(glow);
        } else parent.setEffect(null);

        Label sideTag = lbl(isPlayer ? "YOU" : "OPPONENT", 10, FontWeight.BOLD, GameApp.ICE, "Verdana");
        Label name = lbl(m.getName(), 18, FontWeight.BOLD, Color.WHITE, "Georgia");
        name.setWrapText(true);
        name.setMaxWidth(218);

        Label typeTag = pill(typeOf(m), Color.web("#5A6BAA"));

        HBox roleRow = new HBox(8);
        roleRow.setAlignment(Pos.CENTER_LEFT);
        roleRow.getChildren().add(pill(m.getOriginalRole().toString(), roleColor));
        if (m.getRole() != m.getOriginalRole()) {
            Label arrow = lbl("->", 12, FontWeight.BOLD, GameApp.CORAL, "Verdana");
            Color curColor = m.getRole() == Role.SCARER ? GameApp.SCARER_C : GameApp.LAUGHER_C;
            roleRow.getChildren().addAll(arrow, pill(m.getRole() + " (confused)", curColor));
        }

        Label energyLabel = lbl("Energy", 10, FontWeight.BOLD, GameApp.ICE, "Verdana");
        Label energyVal = lbl(m.getEnergy() + " / " + Constants.WINNING_ENERGY,
            16, FontWeight.BOLD, Color.WHITE, "Verdana");
        StackPane bar = energyBar(m.getEnergy());
        Label pos = lbl("Cell " + m.getPosition(), 12, FontWeight.BOLD, GameApp.ICE, "Verdana");

        HBox status = new HBox(6);
        status.setAlignment(Pos.CENTER_LEFT);
        if (m.isShielded()) status.getChildren().add(chip("SHIELD", Color.web("#3FB8AF")));
        if (m.isFrozen())   status.getChildren().add(chip("FROZEN", Color.web("#7AC0FF")));
        if (m.isConfused()) status.getChildren().add(chip("CONF " + m.getConfusionTurns(),
            Color.web("#C94E8A")));
        if (m instanceof Dasher && ((Dasher) m).getMomentumTurns() > 0)
            status.getChildren().add(chip("MOMENTUM " + ((Dasher) m).getMomentumTurns(),
                GameApp.CORAL));
        if (m instanceof MultiTasker && ((MultiTasker) m).getNormalSpeedTurns() > 0)
            status.getChildren().add(chip("FOCUS " + ((MultiTasker) m).getNormalSpeedTurns(),
                Color.web("#7FD17F")));

        VBox body = new VBox(8);
        body.setPadding(new Insets(16, 16, 16, 16));
        body.getChildren().addAll(sideTag, name, typeTag, roleRow,
            new Region(), energyLabel, energyVal, bar, pos, status);
        VBox.setMargin(typeTag, new Insets(6, 0, 0, 0));
        VBox.setMargin(energyLabel, new Insets(8, 0, 0, 0));

        panel.getChildren().add(body);
    }

    private Label lbl(String text, double size, FontWeight weight, Color color, String family) {
        Label l = new Label(text);
        l.setFont(Font.font(family, weight, size));
        l.setTextFill(color);
        return l;
    }

    private Label pill(String text, Color color) {
        Label l = new Label(text);
        l.setFont(Font.font("Verdana", FontWeight.BOLD, 10));
        l.setTextFill(Color.WHITE);
        l.setPadding(new Insets(3, 9, 3, 9));
        l.setBackground(new Background(new BackgroundFill(color, new CornerRadii(10), Insets.EMPTY)));
        return l;
    }

    private Label chip(String text, Color color) {
        Label l = new Label(text);
        l.setFont(Font.font("Verdana", FontWeight.BOLD, 9));
        l.setTextFill(Color.web("#0F1B3D"));
        l.setPadding(new Insets(2, 7, 2, 7));
        l.setBackground(new Background(new BackgroundFill(color, new CornerRadii(8), Insets.EMPTY)));
        return l;
    }

    private StackPane energyBar(int energy) {
        Rectangle track = new Rectangle(218, 14, Color.web("#101736"));
        track.setArcWidth(10); track.setArcHeight(10);
        double ratio = Math.max(0, Math.min(1.0, energy / (double) Constants.WINNING_ENERGY));
        Color fill = ratio >= 1.0 ? GameApp.GOLD
            : ratio > 0.66 ? Color.web("#7FD17F")
            : ratio > 0.33 ? Color.web("#F2B23A")
            : Color.web("#E45757");
        Rectangle bar = new Rectangle(Math.max(2, ratio * 218), 14, fill);
        bar.setArcWidth(10); bar.setArcHeight(10);
        StackPane s = new StackPane(track, bar);
        StackPane.setAlignment(bar, Pos.CENTER_LEFT);
        s.setMaxWidth(218);
        return s;
    }

    private void setUiLocked(boolean locked) {
        uiLocked = locked;
        boolean frozen = game.getCurrent().isFrozen();
        powerupBtn.setDisable(locked || frozen
            || game.getCurrent().getEnergy() < Constants.POWERUP_COST);
        rollBtn.setDisable(locked || frozen);
        skipBtn.setDisable(locked || !frozen);
    }

    private void onPowerup() {
        if (uiLocked) return;
        Monster c = game.getCurrent();
        HashMap<Monster, Integer> eBefore = snapshotEnergy();
        boolean shieldBefore = c.isShielded();
        boolean oppFrozenBefore = opponentOf(c).isFrozen();
        try {
            game.usePowerup();
        } catch (OutOfEnergyException ex) {
            GameApp.popup("Power-up failed", ex.getMessage()
                + "\n\nAt least " + Constants.POWERUP_COST + " energy is required.");
            return;
        }
        log.add("Turn " + turnNum + ": " + c.getName() + " activated power-up (-"
            + Constants.POWERUP_COST + " energy).");
        if (!shieldBefore && c.isShielded()) log.add("  " + c.getName() + " gained a shield.");
        if (!oppFrozenBefore && opponentOf(c).isFrozen())
            log.add("  " + opponentOf(c).getName() + " was FROZEN.");
        LinkedHashMap<Monster, Integer> deltas = deltas(eBefore);
        for (Monster m : deltas.keySet())
            log.add("  " + m.getName() + " energy " + (deltas.get(m) > 0 ? "+" : "")
                + deltas.get(m) + " (now " + m.getEnergy() + ").");
        refresh();
        playFloats(deltas);
    }

    private void onRoll() {
        if (uiLocked) return;
        setUiLocked(true);
        final Monster current = game.getCurrent();
        final Monster opponent = opponentOf(current);
        final HashMap<Monster, Integer> eBefore = snapshotEnergy();
        final boolean curShield = current.isShielded();
        final boolean oppShield = opponent.isShielded();
        final int curConf = current.getConfusionTurns();
        final int oppConf = opponent.getConfusionTurns();
        final boolean oppFrozen = opponent.isFrozen();
        final HashMap<Integer, Boolean> doorBefore = snapshotDoors();
        final int oldPos = current.getPosition();
        if (Board.getCards().isEmpty()) Board.reloadCards();
        final int deckBefore = Board.getCards().size();
        final Card peeked = Board.getCards().get(0);

        lastRoll = rng.nextInt(6) + 1;
        lastCard = null;

        Timeline shuffle = new Timeline();
        for (int i = 0; i < 10; i++)
            shuffle.getKeyFrames().add(new KeyFrame(Duration.millis(55 * (i + 1)),
                e -> setDice(1 + (int) (Math.random() * 6))));

        shuffle.setOnFinished(e -> {
            try {
                game.getBoard().moveMonster(current, lastRoll, opponent);
            } catch (InvalidMoveException ex) {
                log.add("  Invalid move (roll " + lastRoll + "): " + ex.getMessage());
                refresh();
                setUiLocked(false);
                GameApp.popup("Invalid move", ex.getMessage()
                    + "\n\nLanding on the opponent's cell is not allowed. The roll is rejected;"
                    + " your turn does not advance and you can roll again.");
                return;
            }

            setDice(lastRoll);
            ScaleTransition bounce = new ScaleTransition(Duration.millis(160), dice);
            bounce.setFromX(1); bounce.setFromY(1);
            bounce.setToX(1.2); bounce.setToY(1.2);
            bounce.setAutoReverse(true);
            bounce.setCycleCount(2);
            bounce.play();

            log.add("Turn " + turnNum + ": " + current.getName() + " rolled " + lastRoll
                + " - cell " + oldPos + " -> " + current.getPosition()
                + " (moved " + (current.getPosition() - oldPos) + ").");

            Cell landed = cellAt(current.getPosition());
            boolean doorActivated = false;
            if (Board.getCards().size() < deckBefore) {
                lastCard = peeked;
                log.add("  Drew card: " + peeked.getName() + " - " + peeked.getDescription());
            }
            if (landed instanceof DoorCell) {
                boolean was = doorBefore.getOrDefault(current.getPosition(), false);
                if (!was && ((DoorCell) landed).isActivated()) doorActivated = true;
            }
            LinkedHashMap<Monster, Integer> deltas = deltas(eBefore);
            for (Monster m : deltas.keySet())
                log.add("  " + m.getName() + " energy " + (deltas.get(m) > 0 ? "+" : "")
                    + deltas.get(m) + " (now " + m.getEnergy() + ").");
            if (curShield && !current.isShielded()) log.add("  " + current.getName() + "'s shield consumed.");
            if (oppShield && !opponent.isShielded()) log.add("  " + opponent.getName() + "'s shield consumed.");
            if (!curShield && current.isShielded()) log.add("  " + current.getName() + " gained a shield.");
            if (!oppShield && opponent.isShielded()) log.add("  " + opponent.getName() + " gained a shield.");
            if (curConf == 0 && current.isConfused())
                log.add("  " + current.getName() + " is CONFUSED for "
                    + current.getConfusionTurns() + " turns - roles swapped.");
            if (oppConf == 0 && opponent.isConfused())
                log.add("  " + opponent.getName() + " is CONFUSED for "
                    + opponent.getConfusionTurns() + " turns - roles swapped.");
            if (!oppFrozen && opponent.isFrozen())
                log.add("  " + opponent.getName() + " is FROZEN for next turn.");

            turnNum++;
            game.setCurrent(opponent);

            final boolean da = doorActivated;
            final int pos = current.getPosition();
            PauseTransition pause = new PauseTransition(Duration.millis(220));
            pause.setOnFinished(ev -> {
                refresh();
                if (da) flashCell(pos);
                ParallelTransition followups = new ParallelTransition();
                for (Map.Entry<Monster, Integer> en : deltas.entrySet()) {
                    Animation a = floatEnergy(en.getKey(), en.getValue());
                    if (a != null) followups.getChildren().add(a);
                }
                if (lastCard != null) followups.getChildren().add(popupCard(lastCard));
                followups.setOnFinished(ev2 -> { setUiLocked(false); checkWinner(); });
                if (followups.getChildren().isEmpty()) {
                    setUiLocked(false);
                    checkWinner();
                } else followups.play();
            });
            pause.play();
        });
        shuffle.play();
    }

    private void onSkip() {
        if (uiLocked) return;
        Monster c = game.getCurrent();
        log.add("Turn " + turnNum + ": " + c.getName() + " is FROZEN. Turn skipped.");
        c.setFrozen(false);
        turnNum++;
        game.setCurrent(opponentOf(c));
        lastRoll = 0;
        lastCard = null;
        refresh();
        checkWinner();
    }

    private void playFloats(LinkedHashMap<Monster, Integer> deltas) {
        ParallelTransition pt = new ParallelTransition();
        for (Map.Entry<Monster, Integer> en : deltas.entrySet()) {
            Animation a = floatEnergy(en.getKey(), en.getValue());
            if (a != null) pt.getChildren().add(a);
        }
        pt.setOnFinished(e -> checkWinner());
        if (pt.getChildren().isEmpty()) checkWinner();
        else pt.play();
    }

    private Animation floatEnergy(Monster m, int delta) {
        StackPane wrap = m == game.getPlayer() ? playerWrap
            : m == game.getOpponent() ? opponentWrap : null;
        if (wrap == null) return null;

        Label l = new Label((delta > 0 ? "+" : "") + delta);
        l.setFont(Font.font("Verdana", FontWeight.BOLD, 22));
        l.setTextFill(delta > 0 ? Color.web("#7FD17F") : Color.web("#FF7B7B"));
        DropShadow ds = new DropShadow(14, Color.web("#000", 0.7));
        l.setEffect(ds);

        Bounds b = wrap.localToScene(wrap.getLayoutBounds());
        Bounds bo = overlay.sceneToLocal(b);
        l.setLayoutX(bo.getMinX() + bo.getWidth() / 2 - 24);
        l.setLayoutY(bo.getMinY() + 18);
        l.setOpacity(0);
        overlay.getChildren().add(l);

        TranslateTransition tt = new TranslateTransition(Duration.millis(1100), l);
        tt.setFromY(0); tt.setToY(-60);
        Timeline tl = new Timeline(
            new KeyFrame(Duration.millis(0), new KeyValue(l.opacityProperty(), 0)),
            new KeyFrame(Duration.millis(180), new KeyValue(l.opacityProperty(), 1)),
            new KeyFrame(Duration.millis(900), new KeyValue(l.opacityProperty(), 1)),
            new KeyFrame(Duration.millis(1200), new KeyValue(l.opacityProperty(), 0)));
        ParallelTransition pt = new ParallelTransition(tt, tl);
        pt.setOnFinished(e -> overlay.getChildren().remove(l));
        return pt;
    }

    private Animation popupCard(Card card) {
        VBox cardBox = new VBox(10);
        cardBox.setAlignment(Pos.CENTER);
        cardBox.setPadding(new Insets(22, 28, 22, 28));
        cardBox.setMinSize(340, 220);
        cardBox.setMaxSize(340, 220);
        Color top = cardColor(card);
        LinearGradient g = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, top.brighter()), new Stop(1, top.darker()));
        cardBox.setBackground(new Background(new BackgroundFill(g, new CornerRadii(20), Insets.EMPTY)));
        cardBox.setBorder(new Border(new BorderStroke(Color.WHITE,
            BorderStrokeStyle.SOLID, new CornerRadii(20), new BorderWidths(2))));
        DropShadow ds = new DropShadow(28, Color.web("#000", 0.7));
        ds.setOffsetY(6);
        cardBox.setEffect(ds);

        Label tag = lbl("CARD DRAWN", 12, FontWeight.BOLD, GameApp.GOLD, "Verdana");
        Label name = lbl(card.getName(), 26, FontWeight.BOLD, Color.WHITE, "Georgia");
        name.setWrapText(true);
        name.setTextAlignment(TextAlignment.CENTER);
        name.setMaxWidth(280);

        Label desc = lbl(card.getDescription(), 13, FontWeight.NORMAL, Color.web("#FFEED5"), "Verdana");
        desc.setWrapText(true);
        desc.setTextAlignment(TextAlignment.CENTER);
        desc.setMaxWidth(280);

        Label luck = lbl(card.isLucky() ? "+ LUCKY" : "- UNLUCKY", 11, FontWeight.BOLD,
            card.isLucky() ? Color.web("#7FD17F") : Color.web("#FF9595"), "Verdana");

        cardBox.getChildren().addAll(tag, name, desc, luck);

        cardBox.setLayoutX((GameApp.W - 340) / 2.0);
        cardBox.setLayoutY((GameApp.H - 220) / 2.0 - 30);
        cardBox.setOpacity(0);
        cardBox.setScaleX(0.6); cardBox.setScaleY(0.6);
        overlay.getChildren().add(cardBox);

        ScaleTransition inS = new ScaleTransition(Duration.millis(280), cardBox);
        inS.setFromX(0.6); inS.setFromY(0.6); inS.setToX(1); inS.setToY(1);
        FadeTransition inF = new FadeTransition(Duration.millis(220), cardBox);
        inF.setFromValue(0); inF.setToValue(1);
        PauseTransition hold = new PauseTransition(Duration.millis(1500));
        ScaleTransition outS = new ScaleTransition(Duration.millis(280), cardBox);
        outS.setFromX(1); outS.setFromY(1); outS.setToX(0.85); outS.setToY(0.85);
        FadeTransition outF = new FadeTransition(Duration.millis(280), cardBox);
        outF.setFromValue(1); outF.setToValue(0);
        SequentialTransition all = new SequentialTransition(
            new ParallelTransition(inS, inF), hold, new ParallelTransition(outS, outF));
        all.setOnFinished(e -> overlay.getChildren().remove(cardBox));
        return all;
    }

    private Color cardColor(Card c) {
        if (c instanceof SwapperCard)     return Color.web("#5468C9");
        if (c instanceof ShieldCard)      return Color.web("#3FB8AF");
        if (c instanceof EnergyStealCard) return Color.web("#C94E8A");
        if (c instanceof StartOverCard)   return Color.web("#E58A2E");
        if (c instanceof ConfusionCard)   return Color.web("#8E59C9");
        return Color.web("#5468C9");
    }

    private void flashCell(int idx) {
        BoardCell c = cells[idx];
        ScaleTransition st = new ScaleTransition(Duration.millis(220), c);
        st.setFromX(1); st.setFromY(1);
        st.setToX(1.18); st.setToY(1.18);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
    }

    private void checkWinner() {
        Monster w = game.getWinner();
        if (w == null) return;
        Monster loser = w == game.getPlayer() ? game.getOpponent() : game.getPlayer();
        PauseTransition pt = new PauseTransition(Duration.millis(380));
        pt.setOnFinished(e -> app.showEnd(w, loser));
        pt.play();
    }

    private void refresh() {
        Monster current = game.getCurrent();
        Monster player = game.getPlayer();
        Monster opponent = game.getOpponent();

        hTurn.setText("Turn " + turnNum);
        hCurrent.setText("Active: " + current.getName() + " (" + current.getRole()
            + (current.getRole() != current.getOriginalRole() ? " - confused" : "") + ")");
        hDeck.setText("Deck: " + Board.getCards().size() + " cards");

        for (int i = 0; i < Constants.BOARD_SIZE; i++)
            cells[i].render(cellAt(i), player, opponent);

        populatePanel(playerBox, player, current == player, true);
        populatePanel(opponentBox, opponent, current == opponent, false);

        if (lastRoll > 0) setDice(lastRoll); else setDice(0);

        boolean frozen = current.isFrozen();
        if (!uiLocked) {
            powerupBtn.setDisable(frozen || current.getEnergy() < Constants.POWERUP_COST);
            rollBtn.setDisable(frozen);
            skipBtn.setDisable(!frozen);
        }

        logBox.getChildren().clear();
        int from = Math.max(0, log.size() - 4);
        for (int i = from; i < log.size(); i++) {
            Label l = new Label(log.get(i));
            l.setFont(Font.font("Consolas", 11));
            l.setTextFill(i == log.size() - 1 ? Color.WHITE : Color.web("#9AB0E0"));
            l.setWrapText(true);
            l.setMaxWidth(GameApp.W - 60);
            logBox.getChildren().add(l);
        }
    }

    private Monster opponentOf(Monster m) {
        return m == game.getPlayer() ? game.getOpponent() : game.getPlayer();
    }

    private LinkedHashMap<Monster, Integer> deltas(HashMap<Monster, Integer> before) {
        LinkedHashMap<Monster, Integer> map = new LinkedHashMap<Monster, Integer>();
        ArrayList<Monster> all = new ArrayList<Monster>();
        all.add(game.getPlayer()); all.add(game.getOpponent());
        for (Monster m : Board.getStationedMonsters()) if (!all.contains(m)) all.add(m);
        for (Monster m : all) {
            int b = before.getOrDefault(m, m.getEnergy());
            int d = m.getEnergy() - b;
            if (d != 0) map.put(m, d);
        }
        return map;
    }

    private HashMap<Monster, Integer> snapshotEnergy() {
        HashMap<Monster, Integer> map = new HashMap<Monster, Integer>();
        map.put(game.getPlayer(), game.getPlayer().getEnergy());
        map.put(game.getOpponent(), game.getOpponent().getEnergy());
        for (Monster m : Board.getStationedMonsters()) map.put(m, m.getEnergy());
        return map;
    }

    private HashMap<Integer, Boolean> snapshotDoors() {
        HashMap<Integer, Boolean> map = new HashMap<Integer, Boolean>();
        for (int i = 0; i < Constants.BOARD_SIZE; i++) {
            Cell c = cellAt(i);
            if (c instanceof DoorCell) map.put(i, ((DoorCell) c).isActivated());
        }
        return map;
    }

    private Cell cellAt(int index) {
        int[] rc = indexToRowCol(index);
        return game.getBoard().getBoardCells()[rc[0]][rc[1]];
    }

    private static int[] indexToRowCol(int index) {
        int cols = Constants.BOARD_COLS;
        int row = index / cols;
        int col = index % cols;
        if (row % 2 == 1) col = cols - 1 - col;
        return new int[] { row, col };
    }

    static String typeOf(Monster m) {
        if (m instanceof Dasher) return "Dasher";
        if (m instanceof MultiTasker) return "MultiTasker";
        return m.getClass().getSimpleName();
    }
}