package controller;

import domain.Arbiter;
import domain.Score;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import message.*;
import utils.StringUtils;

import java.util.Collection;
import java.util.List;

import static utils.FxUtils.*;

public class ArbiterController implements ClientMessageReceiver {
    public static final String VIEW_NAME = "/ArbiterView.fxml";
    public static final String VIEW_TITLE = "Arbiter";

    private final ClientMessageHandler messageHandler;
    private Arbiter arbiter;

    @FXML
    private TableView<Score> participantsTable;

    @FXML
    private TableColumn<Score, String> participantsNameColumn;

    @FXML
    private TableColumn<Score, Integer> participantsTypeScoreColumn;

    @FXML
    private TableColumn<Score, Integer> participantsTotalScoreColumn;

    @FXML
    private TableView<Score> rankingTable;

    @FXML
    private TableColumn<Score, String> rankingNameColumn;

    @FXML
    private TableColumn<Score, Integer> rankingScoreColumn;

    @FXML
    private TextField pointsField;

    @FXML
    private Button setButton;

    @FXML
    private ToggleButton showRankingsButton;

    @FXML
    private Label arbiterNameField;

    @FXML
    private Label arbiterTypeField;

    public ArbiterController(ClientMessageHandler messageHandler, Arbiter arbiter) {
        this.messageHandler = messageHandler;
        this.arbiter = arbiter;
    }

    @FXML
    public void initialize() {
        this.arbiterNameField.setText(arbiter.getName());
        this.arbiterTypeField.setText(arbiter.getType().toString().toLowerCase());

        addFieldNumber(pointsField);
        addTableDeselect(participantsTable);
        addTableUnselectable(rankingTable);
        setRankingsTableVisibility(false);

        participantsTypeScoreColumn.setText(String.format("%s score",
                StringUtils.toTitleCase(arbiter.getType().toString())));
        participantsNameColumn.setCellValueFactory(scoreCellDataFeatures -> {
            Score score = scoreCellDataFeatures.getValue();
            return new ReadOnlyStringWrapper(score.getParticipant().getName());
        });
        participantsTypeScoreColumn.setCellValueFactory(scoreCellDataFeatures -> {
            Score score = scoreCellDataFeatures.getValue();
            return new ReadOnlyObjectWrapper<>(score.getScore(arbiter.getType()));
        });
        participantsTotalScoreColumn.setCellValueFactory(scoreCellDataFeatures -> {
            Score score = scoreCellDataFeatures.getValue();
            return new ReadOnlyObjectWrapper<>(score.getTotalScore());
        });
        participantsTable.getSortOrder().add(participantsNameColumn);
        participantsTable.sort();

        rankingNameColumn.setCellValueFactory(scoreCellDataFeatures -> {
            Score score = scoreCellDataFeatures.getValue();
            return new ReadOnlyStringWrapper(score.getParticipant().getName());
        });

        rankingScoreColumn.setSortType(TableColumn.SortType.DESCENDING);
        rankingScoreColumn.setCellValueFactory(scoreCellDataFeatures -> {
            Score score = scoreCellDataFeatures.getValue();
            return new ReadOnlyObjectWrapper<>(score.getScore(arbiter.getType()));
        });
        rankingTable.getSortOrder().add(rankingScoreColumn);
        rankingTable.sort();

        participantsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> onSelectionChanged());

        requestTableData();

        messageHandler.addReceiver(this);
    }

    public void setRankingsTableVisibility(boolean visible) {
        rankingTable.setManaged(visible);
        rankingTable.setVisible(visible);
    }

    public Score getSelected() {
        return participantsTable.getSelectionModel().getSelectedItem();
    }

    public Score findScoreInTable(TableView<Score> table, Score score) {
        Collection<Score> scores = table.getItems();
        for (Score s : scores) {
            if (s.getParticipant().getId() == score.getParticipant().getId()) {
                return s;
            }
        }

        return null;
    }

    public void setParticipantsScore(Score score) {
        Score scoreItem = findScoreInTable(participantsTable, score);
        if (scoreItem != null) {
            scoreItem.setScores(score.getScores());
        }

        participantsTable.refresh();
    }

    public void setRankingScore(Score score) {
        Score scoreItem = findScoreInTable(rankingTable, score);

        boolean isZeroScore = score.getScore(arbiter.getType()) == 0;
        boolean isNullItem = scoreItem == null;
        if (isZeroScore && !isNullItem) {
            rankingTable.getItems().remove(scoreItem);
        } else if (!isZeroScore && !isNullItem) {
            scoreItem.setScores(score.getScores());
        } else if (!isZeroScore) {
            rankingTable.getItems().add(score);
        }

        participantsTable.refresh();
        rankingTable.sort();
    }

    public void setScore(Score score) {
        setParticipantsScore(score);
        setRankingScore(score);
    }

    private void requestParticipantsTableData() {
        messageHandler.requestParticipantScores();
    }

    private void requestRankingTableData() {
        messageHandler.requestRankingScores();
    }

    private void requestTableData() {
        requestParticipantsTableData();
        requestRankingTableData();
    }

    private void setParticipantsTableData(List<Score> scores) {
        participantsTable.getItems().setAll(scores);
    }

    private void setRankingTableData(List<Score> scores) {
        rankingTable.getItems().setAll(scores);
    }

    void onSelectionChanged() {
        Score selected = getSelected();
        if (selected == null) {
            setButton.setDisable(true);
            pointsField.setText("");
        } else {
            setButton.setDisable(false);
            pointsField.setText(String.valueOf(selected.getScore(arbiter.getType())));
        }
    }

    @FXML
    void onSetButtonAction(ActionEvent event) {
        messageHandler.requestSetScore(new ScoreSetData(getSelected().getParticipant().getId(),
                Integer.parseInt(pointsField.getText())));
    }

    @FXML
    void onRefreshButtonAction(ActionEvent event) {
        requestTableData();
    }

    @FXML
    void onShowRankingsButtonAction(ActionEvent event) {
        setRankingsTableVisibility(showRankingsButton.isSelected());
    }

    @Override
    public void onParticipantScoresRequestError(String error) {
        Platform.runLater(() -> showErrorAlert(error));
    }

    @Override
    public void onParticipantScoresResponse(List<Score> scores) {
        Platform.runLater(() -> setParticipantsTableData(scores));
    }

    @Override
    public void onRankingScoresRequestError(String error) {
        Platform.runLater(() -> showErrorAlert(error));
    }

    @Override
    public void onRankingScoresResponse(List<Score> scores) {
        Platform.runLater(() -> setRankingTableData(scores));
    }

    @Override
    public void onSetScoreRequestError(String error) {
        Platform.runLater(() -> showErrorAlert(error));
    }

    @Override
    public void onSetScoreBroadcast(Score score) {
        Platform.runLater(() -> setScore(score));
    }
}
