package com.sicpa.fxagent.testapp;

import javafx.application.Application;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ShowcaseApp extends Application {

    @Override
    public void start(Stage stage) {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setId("tabPane");

        tabPane.getTabs().addAll(
                createTextInputsTab(),
                createButtonsTab(),
                createSelectionTab(),
                createDisplayTab(),
                createListsTab()
        );

        ScrollPane scrollPane = new ScrollPane(tabPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setId("mainScrollPane");

        Scene scene = new Scene(scrollPane, 900, 700);
        stage.setScene(scene);
        stage.setTitle("FxAgent Showcase");
        stage.show();
    }

    private Tab createTextInputsTab() {
        Label title = new Label("Text Input Controls");
        title.getStyleClass().add("section-title");
        title.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        TextField textField = new TextField();
        textField.setId("textField");
        textField.setPromptText("Enter text...");

        Label textFieldEcho = new Label("");
        textFieldEcho.setId("textFieldEcho");
        textField.textProperty().addListener((obs, old, newVal) -> textFieldEcho.setText(newVal));

        PasswordField passwordField = new PasswordField();
        passwordField.setId("passwordField");
        passwordField.setPromptText("Password...");

        Label passwordEcho = new Label("");
        passwordEcho.setId("passwordEcho");
        passwordField.textProperty().addListener((obs, old, newVal) -> passwordEcho.setText(newVal));

        TextArea textArea = new TextArea();
        textArea.setId("textArea");
        textArea.setPromptText("Multi-line...");
        textArea.setPrefRowCount(3);

        Label textAreaEcho = new Label("");
        textAreaEcho.setId("textAreaEcho");
        textArea.textProperty().addListener((obs, old, newVal) -> textAreaEcho.setText(newVal));

        VBox content = new VBox(10,
                title,
                new HBox(10, new Label("TextField:"), textField, textFieldEcho),
                new HBox(10, new Label("Password:"), passwordField, passwordEcho),
                new Label("TextArea:"), textArea, new HBox(10, new Label("Echo:"), textAreaEcho)
        );
        content.setPadding(new Insets(15));

        Tab tab = new Tab("Text Inputs", content);
        return tab;
    }

    private Tab createButtonsTab() {
        Label title = new Label("Button Controls");
        title.getStyleClass().add("section-title");
        title.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        Button button = new Button("Click Me");
        button.setId("button");
        Label buttonStatus = new Label("Ready");
        buttonStatus.setId("buttonStatus");
        button.setOnAction(e -> buttonStatus.setText("Clicked"));

        Button counterButton = new Button("Increment");
        counterButton.setId("counterButton");
        Label counterLabel = new Label("Count: 0");
        counterLabel.setId("counterLabel");
        int[] counter = {0};
        counterButton.setOnAction(e -> {
            counter[0]++;
            counterLabel.setText("Count: " + counter[0]);
        });

        ToggleButton toggleButton = new ToggleButton("Toggle");
        toggleButton.setId("toggleButton");
        Label toggleStatus = new Label("OFF");
        toggleStatus.setId("toggleStatus");
        toggleButton.selectedProperty().addListener((obs, old, selected) ->
                toggleStatus.setText(selected ? "ON" : "OFF"));

        Hyperlink hyperlink = new Hyperlink("Visit Link");
        hyperlink.setId("hyperlink");
        Label hyperlinkStatus = new Label("Not visited");
        hyperlinkStatus.setId("hyperlinkStatus");
        hyperlink.setOnAction(e -> hyperlinkStatus.setText("Visited"));

        VBox content = new VBox(10,
                title,
                new HBox(10, button, buttonStatus),
                new HBox(10, counterButton, counterLabel),
                new HBox(10, toggleButton, toggleStatus),
                new HBox(10, hyperlink, hyperlinkStatus)
        );
        content.setPadding(new Insets(15));

        return new Tab("Buttons", content);
    }

    private Tab createSelectionTab() {
        Label title = new Label("Selection Controls");
        title.getStyleClass().add("section-title");
        title.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        CheckBox checkBox = new CheckBox("Accept Terms");
        checkBox.setId("checkBox");
        Label checkBoxStatus = new Label("Unchecked");
        checkBoxStatus.setId("checkBoxStatus");
        checkBox.selectedProperty().addListener((obs, old, selected) ->
                checkBoxStatus.setText(selected ? "Checked" : "Unchecked"));

        ToggleGroup radioGroup = new ToggleGroup();
        RadioButton radioOption1 = new RadioButton("Option A");
        radioOption1.setId("radioOption1");
        radioOption1.setToggleGroup(radioGroup);
        radioOption1.setSelected(true);

        RadioButton radioOption2 = new RadioButton("Option B");
        radioOption2.setId("radioOption2");
        radioOption2.setToggleGroup(radioGroup);

        RadioButton radioOption3 = new RadioButton("Option C");
        radioOption3.setId("radioOption3");
        radioOption3.setToggleGroup(radioGroup);

        Label radioStatus = new Label("Option A");
        radioStatus.setId("radioStatus");
        radioGroup.selectedToggleProperty().addListener((obs, old, newToggle) -> {
            if (newToggle instanceof RadioButton rb) {
                radioStatus.setText(rb.getText());
            }
        });

        ComboBox<String> comboBox = new ComboBox<>(
                FXCollections.observableArrayList("Apple", "Banana", "Cherry", "Date"));
        comboBox.setId("comboBox");
        comboBox.setPromptText("Select fruit...");

        Label comboStatus = new Label("None");
        comboStatus.setId("comboStatus");
        comboBox.valueProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) comboStatus.setText(newVal);
        });

        ChoiceBox<String> choiceBox = new ChoiceBox<>(
                FXCollections.observableArrayList("Small", "Medium", "Large"));
        choiceBox.setId("choiceBox");

        Label choiceStatus = new Label("None");
        choiceStatus.setId("choiceStatus");
        choiceBox.valueProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) choiceStatus.setText(newVal);
        });

        VBox content = new VBox(10,
                title,
                new HBox(10, checkBox, checkBoxStatus),
                new HBox(10, new Label("Radio:"), radioOption1, radioOption2, radioOption3, radioStatus),
                new HBox(10, new Label("ComboBox:"), comboBox, comboStatus),
                new HBox(10, new Label("ChoiceBox:"), choiceBox, choiceStatus)
        );
        content.setPadding(new Insets(15));

        return new Tab("Selection", content);
    }

    private Tab createDisplayTab() {
        Label title = new Label("Display & Range Controls");
        title.getStyleClass().add("section-title");
        title.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        Label displayLabel = new Label("Hello, JavaFX!");
        displayLabel.setId("displayLabel");
        displayLabel.setStyle("-fx-font-size: 18;");

        ProgressBar progressBar = new ProgressBar(0.65);
        progressBar.setId("progressBar");
        progressBar.setPrefWidth(300);

        ProgressIndicator progressIndicator = new ProgressIndicator(0.65);
        progressIndicator.setId("progressIndicator");
        progressIndicator.setPrefSize(60, 60);

        Slider slider = new Slider(0, 100, 50);
        slider.setId("slider");
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setPrefWidth(300);

        Label sliderValue = new Label("50");
        sliderValue.setId("sliderValue");
        slider.valueProperty().addListener((obs, old, newVal) ->
                sliderValue.setText(String.valueOf(newVal.intValue())));

        VBox content = new VBox(10,
                title,
                new HBox(10, new Label("Label:"), displayLabel),
                new HBox(10, new Label("ProgressBar:"), progressBar),
                new HBox(10, new Label("ProgressIndicator:"), progressIndicator),
                new HBox(10, new Label("Slider:"), slider, sliderValue)
        );
        content.setPadding(new Insets(15));

        return new Tab("Display", content);
    }

    @SuppressWarnings("unchecked")
    private Tab createListsTab() {
        Label title = new Label("List & Table Controls");
        title.getStyleClass().add("section-title");
        title.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        ListView<String> listView = new ListView<>(
                FXCollections.observableArrayList("Item 1", "Item 2", "Item 3", "Item 4", "Item 5"));
        listView.setId("listView");
        listView.setPrefHeight(120);

        Label listSelectionStatus = new Label("None selected");
        listSelectionStatus.setId("listSelectionStatus");
        listView.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) listSelectionStatus.setText(newVal);
        });

        TableView<Person> tableView = new TableView<>();
        tableView.setId("tableView");
        tableView.setPrefHeight(150);

        TableColumn<Person, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(150);

        TableColumn<Person, Integer> ageCol = new TableColumn<>("Age");
        ageCol.setCellValueFactory(new PropertyValueFactory<>("age"));
        ageCol.setPrefWidth(80);

        TableColumn<Person, String> cityCol = new TableColumn<>("City");
        cityCol.setCellValueFactory(new PropertyValueFactory<>("city"));
        cityCol.setPrefWidth(150);

        tableView.getColumns().addAll(nameCol, ageCol, cityCol);
        tableView.setItems(FXCollections.observableArrayList(
                new Person("Alice", 30, "Paris"),
                new Person("Bob", 25, "London"),
                new Person("Charlie", 35, "Berlin"),
                new Person("Diana", 28, "Madrid")
        ));

        Label tableSelectionStatus = new Label("None selected");
        tableSelectionStatus.setId("tableSelectionStatus");
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) tableSelectionStatus.setText(newVal.getName());
        });

        VBox content = new VBox(10,
                title,
                new Label("ListView:"), listView,
                new HBox(10, new Label("Selected:"), listSelectionStatus),
                new Label("TableView:"), tableView,
                new HBox(10, new Label("Selected:"), tableSelectionStatus)
        );
        content.setPadding(new Insets(15));

        return new Tab("Lists", content);
    }

    public static class Person {
        private final SimpleStringProperty name;
        private final SimpleIntegerProperty age;
        private final SimpleStringProperty city;

        public Person(String name, int age, String city) {
            this.name = new SimpleStringProperty(name);
            this.age = new SimpleIntegerProperty(age);
            this.city = new SimpleStringProperty(city);
        }

        public String getName() { return name.get(); }
        public int getAge() { return age.get(); }
        public String getCity() { return city.get(); }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
