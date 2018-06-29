package fxsimulator;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXNodesList;
import com.jfoenix.controls.JFXSlider;
import com.jfoenix.controls.JFXToggleButton;
import java.awt.Point;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.FillTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.StrokeTransition;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

public class CanvasController implements Initializable, ChangeListener {

    @FXML
    private JFXButton canvasBackButton, clearButton, resetButton, gear, playPauseButton;
    @FXML
    private JFXToggleButton addNodeButton, addEdgeButton, bfsButton, dfsButton, dijkstraButton, articulationPointButton;
    @FXML
    private ToggleGroup algoToggleGroup;
    @FXML
    private Pane viewer;
    @FXML
    private Group canvasGroup;
    @FXML
    private Line edgeLine;
    @FXML
    private Label sourceText = new Label("Source"), weight;
    @FXML
    private Pane border;
    @FXML
    private Arrow arrow;
    @FXML
    private JFXNodesList nodeList;
    @FXML
    private JFXSlider slider;
    @FXML
    private ImageView playPauseImage;

    int nNode = 0, time = 500;
    NodeFX selectedNode = null, articulationStart = null;
    List<NodeFX> circles = new ArrayList<NodeFX>();
    List<Edge> mstEdges = new ArrayList<Edge>();
    List<Shape> edges = new ArrayList<Shape>();
    boolean addNode = true, addEdge = false, calculate = false,
            calculated = false, playing = false, paused = false;
    List<Label> distances = new ArrayList<Label>(), visitTime = new ArrayList<Label>(), lowTime = new ArrayList<Label>();
    private boolean weighted = Panel1Controller.weighted, unweighted = Panel1Controller.unweighted,
            directed = Panel1Controller.directed, undirected = Panel1Controller.undirected,
            bfs = true, dfs = true, dijkstra = true, articulationPoint = true;
    Algorithm algo = new Algorithm();
    SequentialTransition st;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ResetHandle(null);
        viewer.prefHeightProperty().bind(border.heightProperty());
        viewer.prefWidthProperty().bind(border.widthProperty());
//        AddNodeHandle(null);
        addEdgeButton.setDisable(true);
        clearButton.setDisable(true);

        if (weighted) {
            bfsButton.setDisable(true);
            dfsButton.setDisable(true);
            articulationPointButton.setDisable(true);
        }
        if (unweighted) {
            dijkstraButton.setDisable(true);
        }
        if(directed)
            articulationPointButton.setDisable(true);
        canvasBackButton.setOnAction(e -> {
            try {
                ResetHandle(null);
                Parent root = FXMLLoader.load(getClass().getResource("Panel1FXML.fxml"));

                Scene scene = new Scene(root);
                FXSimulator.primaryStage.setScene(scene);
            } catch (IOException ex) {
                Logger.getLogger(CanvasController.class.getName()).log(Level.SEVERE, null, ex);
            }
        });

        slider = new JFXSlider(10, 1000, 500);
        slider.setPrefWidth(150);
        slider.setPrefHeight(80);
        slider.setSnapToTicks(true);
        slider.setMinorTickCount(100);
        slider.setIndicatorPosition(JFXSlider.IndicatorPosition.RIGHT);
        slider.setBlendMode(BlendMode.MULTIPLY);
        slider.setCursor(Cursor.CLOSED_HAND);
        nodeList.addAnimatedNode(slider);
        nodeList.setSpacing(50D);
        nodeList.setRotate(270D);

        slider.valueProperty().addListener(this);

    }

    //Listens change in the slider
    @Override
    public void changed(ObservableValue observable, Object oldValue, Object newValue) {
        int temp = (int) slider.getValue();

        if (temp > 500) {
            int diff = temp - 500;
            temp = 500;
            temp -= diff;
            temp += 10;
        } else if (temp < 500) {
            int diff = 500 - temp;
            temp = 500;
            temp += diff;
            temp -= 10;
        }
        time = temp;
        System.out.println(time);
    }

    //Adds new node when clicked on empty canvas
    @FXML
    public void handle(MouseEvent ev) {
        if (addNode) {
            if (nNode == 2) {
                addEdgeButton.setDisable(false);
                AddNodeHandle(null);
            }

            if (!ev.getSource().equals(canvasGroup)) {
                if (ev.getEventType() == MouseEvent.MOUSE_RELEASED && ev.getButton() == MouseButton.PRIMARY) {
                    nNode++;
                    NodeFX circle = new NodeFX(ev.getX(), ev.getY(), 1.2, String.valueOf(nNode));
                    canvasGroup.getChildren().add(circle);
                    if(articulationStart == null)
                        articulationStart = circle;
                    circle.setOnMousePressed(mouseHandler);
                    circle.setOnMouseReleased(mouseHandler);
                    circle.setOnMouseDragged(mouseHandler);
                    circle.setOnMouseExited(mouseHandler);
                    circle.setOnMouseEntered(mouseHandler);

                    ScaleTransition tr = new ScaleTransition(Duration.millis(100), circle);
                    tr.setByX(10f);
                    tr.setByY(10f);
                    tr.setInterpolator(Interpolator.EASE_OUT);
                    tr.play();
                }
            }
        }
    }

    //Selects a node when clicked on a node
    EventHandler<MouseEvent> mouseHandler = new EventHandler<MouseEvent>() {

        @Override
        public void handle(MouseEvent mouseEvent) {
            if (mouseEvent.getEventType() == MouseEvent.MOUSE_PRESSED && mouseEvent.getButton() == MouseButton.PRIMARY) {
                NodeFX circle = (NodeFX) mouseEvent.getSource();

                if (!circle.isSelected) {
                    if (selectedNode != null) {
                        if (addEdge) {
                            weight = new Label();

                            //Adds the edge between two selected nodes
                            if (undirected) {
                                edgeLine = new Line(selectedNode.point.x, selectedNode.point.y, circle.point.x, circle.point.y);
                                canvasGroup.getChildren().add(edgeLine);
                            } else if (directed) {
                                arrow = new Arrow(selectedNode.point.x, selectedNode.point.y, circle.point.x, circle.point.y);
                                canvasGroup.getChildren().add(arrow);
                            }
                            //Adds weight between two selected nodes
                            if (weighted) {
                                weight.setLayoutX(((selectedNode.point.x) + (circle.point.x)) / 2);
                                weight.setLayoutY(((selectedNode.point.y) + (circle.point.y)) / 2);

                                TextInputDialog dialog = new TextInputDialog("0");
                                dialog.setTitle(null);
                                dialog.setHeaderText("Enter Weight of the Edge :");
                                dialog.setContentText(null);

                                Optional<String> result = dialog.showAndWait();
                                if (result.isPresent()) {
                                    weight.setText(result.get());
                                } else {
                                    weight.setText("0");
                                }
                                canvasGroup.getChildren().add(weight);
                            } else if (unweighted) {
                                weight.setText("1");
                            }

                            if (undirected) {
                                Edge temp = new Edge(selectedNode.node, circle.node, Integer.valueOf(weight.getText()), edgeLine);
                                mstEdges.add(temp);
                                selectedNode.node.adjacents.add(new Edge(selectedNode.node, circle.node, Integer.valueOf(weight.getText()), edgeLine));
                                circle.node.adjacents.add(new Edge(circle.node, selectedNode.node, Integer.valueOf(weight.getText()), edgeLine));
                                edges.add(edgeLine);
                            } else if (directed) {
                                selectedNode.node.adjacents.add(new Edge(selectedNode.node, circle.node, Integer.valueOf(weight.getText()), arrow));
                                circle.node.revAdjacents.add(new Edge(circle.node, selectedNode.node, Integer.valueOf(weight.getText()), arrow));
                                edges.add(arrow);
                            }
                        }
                        if (addNode || (calculate && !calculated) || addEdge) {
                            selectedNode.isSelected = false;
                            FillTransition ft1 = new FillTransition(Duration.millis(300), selectedNode, Color.RED, Color.BLACK);
                            ft1.play();
                        }
                        selectedNode = null;
                        return;
                    }

                    FillTransition ft = new FillTransition(Duration.millis(300), circle, Color.BLACK, Color.RED);
                    ft.play();
                    circle.isSelected = true;
                    selectedNode = circle;

                    /**
                     * WHAT TO DO WHEN SELECTED ON ACTIVE ALGORITHM
                     */
                    if (calculate && !calculated) {
                        if (bfs) {
                            algo.newBFS(circle.node);
                        } else if (dfs) {
                            algo.newDFS(circle.node);
                        } else if (dijkstra) {
                            algo.newDijkstra(circle.node);
                        }
//                        else if (articulationPoint) {
//                            algo.newArticulationPoint(circle.node);
//                        }

                        calculated = true;
                    } else if (calculate && calculated && !articulationPoint) {

                        for (NodeFX n : circles) {
                            n.isSelected = false;
                            FillTransition ft1 = new FillTransition(Duration.millis(300), n);
                            ft1.setToValue(Color.BLACK);
                            ft1.play();
                        }
                        List<Node> path = algo.getShortestPathTo(circle.node);
                        for (Node n : path) {
                            FillTransition ft1 = new FillTransition(Duration.millis(300), n.circle);
                            ft1.setToValue(Color.BLUE);
                            ft1.play();
                        }
                    }

                } else {
                    circle.isSelected = false;
                    FillTransition ft1 = new FillTransition(Duration.millis(300), circle, Color.RED, Color.BLACK);
                    ft1.play();
                    selectedNode = null;
                }

            }
        }

    };
    
    @FXML
    public void PlayPauseHandle(ActionEvent event){
        System.out.println("IN PLAYPAUSE");
        System.out.println(playing +" " + paused);
        if(playing){
            Image image = new Image(getClass().getResourceAsStream("/play_arrow_black_48x48.png"));
            playPauseImage.setImage(image);
            System.out.println("Pausing");
            st.pause();
            paused = true;
            playing = false;
            return;
        }
        if(paused){
            Image image = new Image(getClass().getResourceAsStream("/pause_black_48x48.png"));
            playPauseImage.setImage(image);
            st.play();
            playing = true;
            paused = false;
            return;
        }
    }
    @FXML
    public void ResetHandle(ActionEvent event) {
        ClearHandle(null);
        nNode = 0;
        canvasGroup.getChildren().clear();
        canvasGroup.getChildren().addAll(viewer);
        selectedNode = null;
        circles = new ArrayList<NodeFX>();
        distances = new ArrayList<Label>();
        visitTime = new ArrayList<Label>();
        lowTime = new ArrayList<Label>();
        addNode = true;
        addEdge = false;
        calculate = false;
        calculated = false;
        addNodeButton.setSelected(true);
        addEdgeButton.setSelected(false);
        addEdgeButton.setDisable(true);
        addNodeButton.setDisable(false);
        clearButton.setDisable(true);
        algo = new Algorithm();

        bfsButton.setDisable(true);
        dfsButton.setDisable(true);
        dijkstraButton.setDisable(true);
        articulationPointButton.setDisable(true);
        playing = false;
        paused = false;
    }

    @FXML
    public void ClearHandle(ActionEvent event) {
        selectedNode = null;
        calculated = false;
        for (NodeFX n : circles) {
            n.isSelected = false;
            n.node.visited = false;
            n.node.previous = null;
            n.node.minDistance = Double.POSITIVE_INFINITY;

            FillTransition ft1 = new FillTransition(Duration.millis(300), n);
            ft1.setToValue(Color.BLACK);
            ft1.play();
        };

        canvasGroup.getChildren().remove(sourceText);
        for (Label x : distances) {
            x.setText("Distance : INFINITY");
            canvasGroup.getChildren().remove(x);
        }
        for (Label x : visitTime) {
            x.setText("Visit : 0");
            canvasGroup.getChildren().remove(x);
        }
        for (Label x : lowTime) {
            x.setText("Low Value : NULL");
            canvasGroup.getChildren().remove(x);
        }
        distances = new ArrayList<Label>();
        visitTime = new ArrayList<Label>();
        lowTime = new ArrayList<Label>();
        addNodeButton.setDisable(false);
        addEdgeButton.setDisable(false);
        AddNodeHandle(null);
        bfs = false;
        dfs = false;
        articulationPoint = false;
        dijkstra = false;
        playing = false;
        paused = false;
    }

    @FXML
    public void AddEdgeHandle(ActionEvent event) {
        addNode = false;
        addEdge = true;
        calculate = false;
        addNodeButton.setSelected(false);
        addEdgeButton.setSelected(true);

        if (unweighted) {
            bfsButton.setDisable(false);
            bfsButton.setSelected(false);
            dfsButton.setDisable(false);
            dfsButton.setSelected(false);
            articulationPointButton.setDisable(false);
            articulationPointButton.setSelected(false);
        }
        if (weighted) {
            dijkstraButton.setDisable(false);
            dijkstraButton.setSelected(false);
        }
    }

    @FXML
    public void AddNodeHandle(ActionEvent event) {
        addNode = true;
        addEdge = false;
        calculate = false;
        addNodeButton.setSelected(true);
        addEdgeButton.setSelected(false);
        selectedNode = null;

        if (unweighted) {
            bfsButton.setDisable(false);
            bfsButton.setSelected(false);
            dfsButton.setDisable(false);
            dfsButton.setSelected(false);
            if(undirected){
                articulationPointButton.setDisable(false);
                articulationPointButton.setSelected(false);
            }
        }
        if (weighted) {
            dijkstraButton.setDisable(false);
            dijkstraButton.setSelected(false);
        }
    }

    @FXML
    public void BFSHandle(ActionEvent event) {
        addNode = false;
        addEdge = false;
        addNodeButton.setSelected(false);
        addEdgeButton.setSelected(false);
        addNodeButton.setDisable(true);
        addEdgeButton.setDisable(true);
        calculate = true;
        clearButton.setDisable(false);
        bfs = true;
        dfs = false;
        dijkstra = false;
    }

    @FXML
    public void DFSHandle(ActionEvent event) {
        addNode = false;
        addEdge = false;
        addNodeButton.setSelected(false);
        addEdgeButton.setSelected(false);
        addNodeButton.setDisable(true);
        addEdgeButton.setDisable(true);
        calculate = true;
        clearButton.setDisable(false);
        dfs = true;
        bfs = false;
        dijkstra = false;
    }

    @FXML
    public void ArticulationPointHandle(ActionEvent event) {
        addNode = false;
        addEdge = false;
        addNodeButton.setSelected(false);
        addEdgeButton.setSelected(false);;
        addNodeButton.setDisable(true);
        addEdgeButton.setDisable(true);
        calculate = true;
        clearButton.setDisable(false);
        dfs = false;
        bfs = false;
        dijkstra = false;
        articulationPoint = true;
        algo.newArticulationPoint(articulationStart.node);
    }

    @FXML
    public void DijkstraHandle(ActionEvent event) {
        addNode = false;
        addEdge = false;
        addNodeButton.setSelected(false);
        addEdgeButton.setSelected(false);
        addNodeButton.setDisable(true);
        addEdgeButton.setDisable(true);
        calculate = true;
        clearButton.setDisable(false);
        bfs = false;
        dfs = false;
        dijkstra = true;
        articulationPoint = false;
    }

    public class NodeFX extends Circle {

        Node node;
        Point point;
        Label distance = new Label("Dist. : INFINITY");
        Label visitTime = new Label("Visit : 0");
        Label lowTime = new Label("Low : 0");
        boolean isSelected = false;

        public NodeFX(double x, double y, double rad, String name) {
            super(x, y, rad);
            node = new Node(name, this);
            point = new Point((int) x, (int) y);
            Label id = new Label(name);
            canvasGroup.getChildren().add(id);
            id.setLayoutX(x - 18);
            id.setLayoutY(y - 18);
            this.setOpacity(0.5);
            this.setBlendMode(BlendMode.MULTIPLY);
            circles.add(this);
        }
    }

    /*
     Algorithm Declarations -----------------------------------------------
     */
    public class Algorithm {

        

        //<editor-fold defaultstate="collapsed" desc="Dijkstra">
        public void newDijkstra(Node source) {
            new Dijkstra(source);
        }

        class Dijkstra {

            Dijkstra(Node source) {

                //<editor-fold defaultstate="collapsed" desc="Animation Control">
                for (NodeFX n : circles) {
                    distances.add(n.distance);
                    n.distance.setLayoutX(n.point.x + 20);
                    n.distance.setLayoutY(n.point.y);
                    canvasGroup.getChildren().add(n.distance);
                }
                sourceText.setLayoutX(source.circle.point.x + 20);
                sourceText.setLayoutY(source.circle.point.y + 10);
                canvasGroup.getChildren().add(sourceText);
                SequentialTransition st = new SequentialTransition();
                source.circle.distance.setText("Dist. : " + 0);
                //</editor-fold>

                source.minDistance = 0;
                PriorityQueue<Node> pq = new PriorityQueue<Node>();
                pq.add(source);
                while (!pq.isEmpty()) {
                    Node u = pq.poll();
                    System.out.println(u.name);
                    //<editor-fold defaultstate="collapsed" desc="Animation Control">
                    FillTransition ft = new FillTransition(Duration.millis(time), u.circle);
                    ft.setToValue(Color.CHOCOLATE);
                    st.getChildren().add(ft);
                    //</editor-fold>
                    for (Edge e : u.adjacents) {
                        if (e != null) {
                            Node v = e.target;
                            System.out.println("HERE " + v.name);
                            if (u.minDistance + e.weight < v.minDistance) {
                                pq.remove(v);
                                v.minDistance = u.minDistance + e.weight;
                                v.previous = u;
                                pq.add(v);
                                //<editor-fold defaultstate="collapsed" desc="Animation Control">
                                FillTransition ft1 = new FillTransition(Duration.millis(time), v.circle);
                                ft1.setToValue(Color.FORESTGREEN);
                                ft1.setOnFinished(ev -> {
                                    v.circle.distance.setText("Dist. : " + v.minDistance);
                                });
                                ft1.onFinishedProperty();
                                st.getChildren().add(ft1);
                                //</editor-fold>
                            }
                        }
                    }
                    //<editor-fold defaultstate="collapsed" desc="Animation Control">
                    FillTransition ft2 = new FillTransition(Duration.millis(time), u.circle);
                    ft2.setToValue(Color.BLUEVIOLET);
                    st.getChildren().add(ft2);
                    //</editor-fold>
                }

                //<editor-fold defaultstate="collapsed" desc="Animation Control">
                st.setOnFinished(ev -> {
                    for (NodeFX n : circles) {
                        FillTransition ft1 = new FillTransition(Duration.millis(time), n);
                        ft1.setToValue(Color.BLACK);
                        ft1.play();
                    }
                    FillTransition ft1 = new FillTransition(Duration.millis(time), source.circle);
                    ft1.setToValue(Color.RED);
                    ft1.play();
                });
                st.onFinishedProperty();
                st.play();
                playing = true;
                //</editor-fold>
            }
        }
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="BFS">
        public void newBFS(Node source) {
            new BFS(source);
        }

        class BFS {

            BFS(Node source) {

                //<editor-fold defaultstate="collapsed" desc="Set labels and distances">
                for (NodeFX n : circles) {
                    distances.add(n.distance);
                    n.distance.setLayoutX(n.point.x + 20);
                    n.distance.setLayoutY(n.point.y);
                    canvasGroup.getChildren().add(n.distance);
                }
                sourceText.setLayoutX(source.circle.point.x + 20);
                sourceText.setLayoutY(source.circle.point.y + 10);
                canvasGroup.getChildren().add(sourceText);
                st = new SequentialTransition();
                source.circle.distance.setText("Dist. : " + 0);
                //</editor-fold>

                source.minDistance = 0;
                source.visited = true;
                LinkedList<Node> q = new LinkedList<Node>();
                q.push(source);
                while (!q.isEmpty()) {
                    Node u = q.removeLast();
                    //<editor-fold defaultstate="collapsed" desc="Animation Control">
                    FillTransition ft = new FillTransition(Duration.millis(time), u.circle);
                    if (u.circle.getFill() == Color.BLACK) {
                        ft.setToValue(Color.CHOCOLATE);
                    }
                    st.getChildren().add(ft);
                    //</editor-fold>
                    System.out.println(u.name);
                    for (Edge e : u.adjacents) {
                        if (e != null) {
                            Node v = e.target;
                                
                            if (!v.visited) {
                                v.minDistance = u.minDistance + 1;
                                v.visited = true;
                                q.push(v);
                                v.previous = u;

                                //<editor-fold defaultstate="collapsed" desc="Animation Control">
                                //<editor-fold defaultstate="collapsed" desc="Change Edge colors">
                                StrokeTransition ftEdge = new StrokeTransition(Duration.millis(time), e.line);
                                ftEdge.setToValue(Color.rgb(163, 203, 56,1.0));
                                st.getChildren().add(ftEdge);
                                //</editor-fold>
                                FillTransition ft1 = new FillTransition(Duration.millis(time), v.circle);
                                ft1.setToValue(Color.FORESTGREEN);
                                ft1.setOnFinished(ev -> {
                                    v.circle.distance.setText("Dist. : " + v.minDistance);
                                });
                                ft1.onFinishedProperty();
                                st.getChildren().add(ft1);
                                //</editor-fold>
                            }
                        }
                    }
                    //<editor-fold defaultstate="collapsed" desc="Animation Control">
                    FillTransition ft2 = new FillTransition(Duration.millis(time), u.circle);
                    ft2.setToValue(Color.BLUEVIOLET);
                    st.getChildren().add(ft2);
                    //</editor-fold>
                }

                //<editor-fold defaultstate="collapsed" desc="Animation Control">
                st.setOnFinished(ev -> {
                    for (NodeFX n : circles) {
                        FillTransition ft1 = new FillTransition(Duration.millis(time), n);
                        ft1.setToValue(Color.BLACK);
                        ft1.play();
                    }
                    for(Shape n : edges){
                        n.setStroke(Color.BLACK);
                    }
                    FillTransition ft1 = new FillTransition(Duration.millis(time), source.circle);
                    ft1.setToValue(Color.RED);
                    ft1.play();
                });
                st.onFinishedProperty();
                st.play();
                playing = true;
                //</editor-fold>
            }
        }
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="DFS">
        public void newDFS(Node source) {
            new DFS(source);
        }

        class DFS {

            DFS(Node source) {

                //<editor-fold defaultstate="collapsed" desc="Animation Setup Distances">
                for (NodeFX n : circles) {
                    distances.add(n.distance);
                    n.distance.setLayoutX(n.point.x + 20);
                    n.distance.setLayoutY(n.point.y);
                    canvasGroup.getChildren().add(n.distance);
                }
                sourceText.setLayoutX(source.circle.point.x + 20);
                sourceText.setLayoutY(source.circle.point.y + 10);
                canvasGroup.getChildren().add(sourceText);
                st = new SequentialTransition();
                source.circle.distance.setText("Dist. : " + 0);
                //</editor-fold>

                source.minDistance = 0;
                source.visited = true;
                DFSRecursion(source);

                //<editor-fold defaultstate="collapsed" desc="Animation after algorithm is finished">
                st.setOnFinished(ev -> {
                    for (NodeFX n : circles) {
                        FillTransition ft1 = new FillTransition(Duration.millis(time), n);
                        ft1.setToValue(Color.BLACK);
                        ft1.play();
                    }
                    for(Shape n : edges){
                        n.setStroke(Color.BLACK);
                    }
                    FillTransition ft1 = new FillTransition(Duration.millis(time), source.circle);
                    ft1.setToValue(Color.RED);
                    ft1.play();
                });
                st.onFinishedProperty();
                st.play();
                playing = true;
                //</editor-fold>
            }

            public void DFSRecursion(Node source) {
                //<editor-fold defaultstate="collapsed" desc="Animation Control">
                FillTransition ft = new FillTransition(Duration.millis(time), source.circle);
                if (source.circle.getFill() == Color.BLACK) {
                    ft.setToValue(Color.FORESTGREEN);
                }
                st.getChildren().add(ft);
                //</editor-fold>
                for (Edge e : source.adjacents) {
                    if (e != null) {
                        Node v = e.target;
                        if (!v.visited) {
                            v.minDistance = source.minDistance + 1;
                            v.visited = true;
                            v.previous = source;
//                        v.circle.distance.setText("Dist. : " + v.minDistance);
                            //<editor-fold defaultstate="collapsed" desc="Change Edge colors">
                            StrokeTransition ftEdge = new StrokeTransition(Duration.millis(time), e.line);
                            ftEdge.setToValue(Color.FORESTGREEN);
                            st.getChildren().add(ftEdge);
                            //</editor-fold>
                            DFSRecursion(v);
                            //<editor-fold defaultstate="collapsed" desc="Animation Control">
                            //<editor-fold defaultstate="collapsed" desc="Change Edge colors">
                            StrokeTransition ftEdge1 = new StrokeTransition(Duration.millis(time), e.line);
                            ftEdge1.setToValue(Color.BLUEVIOLET);
                            st.getChildren().add(ftEdge1);
                            //</editor-fold>
                            FillTransition ft1 = new FillTransition(Duration.millis(time), v.circle);
                            ft1.setToValue(Color.BLUEVIOLET);
                            ft1.onFinishedProperty();
                            ft1.setOnFinished(ev -> {
                                v.circle.distance.setText("Dist. : " + v.minDistance);
                            });
                            st.getChildren().add(ft1);
                            //</editor-fold>
                        }
                    }
                }
            }
        }
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="Articulation Point">
        public void newArticulationPoint(Node s) {
            new ArticulationPoint(s);
        }

        class ArticulationPoint {

            int timeCnt = 0;

            ArticulationPoint(Node source) {

                //<editor-fold defaultstate="collapsed" desc="Animation Setup Distances">
                for (NodeFX n : circles) {
                    visitTime.add(n.visitTime);
                    n.visitTime.setLayoutX(n.point.x + 20);
                    n.visitTime.setLayoutY(n.point.y);
                    canvasGroup.getChildren().add(n.visitTime);

                    lowTime.add(n.lowTime);
                    n.lowTime.setLayoutX(n.point.x + 20);
                    n.lowTime.setLayoutY(n.point.y + 13);
                    canvasGroup.getChildren().add(n.lowTime);

                    n.node.isArticulationPoint = false;
                }
//                sourceText.setLayoutX(source.circle.point.x + 20);
//                sourceText.setLayoutY(source.circle.point.y + 25);
//                canvasGroup.getChildren().add(sourceText);
                st = new SequentialTransition();
                source.circle.lowTime.setText("Low : " + source.name);
                source.circle.visitTime.setText("Visit : " + source.visitTime);
                //</editor-fold>

                timeCnt = 0;
                RecAP(source);

                for (NodeFX n : circles) {
                    if (n.node.isArticulationPoint) {
                        System.out.println(n.node.name);
                    }
                }

                //<editor-fold defaultstate="collapsed" desc="Animation after algorithm is finished">
                st.setOnFinished(ev -> {
                    for (NodeFX n : circles) {
                        FillTransition ft1 = new FillTransition(Duration.millis(time), n);
                        ft1.setToValue(Color.BLACK);
                        ft1.play();
                    }
                    for(Shape n : edges){
                        n.setStroke(Color.BLACK);
                    }
                    for (NodeFX n : circles) {
                        if (n.node.isArticulationPoint) {
                            FillTransition ft1 = new FillTransition(Duration.millis(time), n);
                            ft1.setToValue(Color.CHARTREUSE);
                            ft1.play();
                        }
                    }
                });
                st.onFinishedProperty();
                st.play();
                playing = true;
                //</editor-fold>
            }

            void RecAP(Node s) {
                //<editor-fold defaultstate="collapsed" desc="Animation Control">
                FillTransition ft = new FillTransition(Duration.millis(time), s.circle);
                if (s.circle.getFill() == Color.BLACK) {
                    ft.setToValue(Color.FORESTGREEN);
                }
                ft.setOnFinished(ev -> {
                    s.circle.lowTime.setText("Low : " + s.lowTime);
                    s.circle.visitTime.setText("Visit : " + s.visitTime);
                });
                st.getChildren().add(ft);
                //</editor-fold>
                s.visited = true;
                s.visitTime = timeCnt;
                s.lowTime = timeCnt;

                timeCnt++;
                int childCount = 0;

                for (Edge e : s.adjacents) {
                    if (e != null) {
                        Node v = e.target;
                        if (s.previous == v) {
                            continue;
                        }
                        if (!v.visited) {
                            v.previous = s;
                            childCount++;
                            //<editor-fold defaultstate="collapsed" desc="Change Edge colors">
                            StrokeTransition ftEdge = new StrokeTransition(Duration.millis(time), e.line);
                            ftEdge.setToValue(Color.FORESTGREEN);
                            st.getChildren().add(ftEdge);
                            //</editor-fold>
                            RecAP(v);

                            s.lowTime = Math.min(s.lowTime, v.lowTime);
                            if (s.visitTime <= v.lowTime && s.previous != null) {
                                s.isArticulationPoint = true;
                            }

                            //<editor-fold defaultstate="collapsed" desc="Animation Control">
                            ///<editor-fold defaultstate="collapsed" desc="Change Edge colors">
                            StrokeTransition ftEdge1 = new StrokeTransition(Duration.millis(time), e.line);
                            ftEdge1.setToValue(Color.BLUEVIOLET);
                            st.getChildren().add(ftEdge1);
                            //</editor-fold>
                            FillTransition ft1 = new FillTransition(Duration.millis(time), v.circle);
                            ft1.setToValue(Color.BLUEVIOLET);
                            ft1.setOnFinished(ev -> {
                                s.circle.lowTime.setText("Low : " + s.lowTime);
                                s.circle.visitTime.setText("Visit : " + s.visitTime);
                            });
                            ft1.onFinishedProperty();
                            st.getChildren().add(ft1);
                            //</editor-fold>
                        } else {
                            s.lowTime = Math.min(s.lowTime, v.visitTime);
                        }
                    }
                }
                if (childCount > 1 && s.previous == null) {
                    s.isArticulationPoint = true;
                }
            }
        }
        //</editor-fold>
        
        //<editor-fold defaultstate="collapsed" desc="Strongly Connected Components">
        class StronglyConnectedComponent{

            public StronglyConnectedComponent() {
                
            }
            
        }
        //</editor-fold>
        
        public List<Node> getShortestPathTo(Node target) {
            List<Node> path = new ArrayList<Node>();
            for (Node i = target; i != null; i = i.previous) {
                path.add(i);
            }
            Collections.reverse(path);
            return path;
        }
    }
}
