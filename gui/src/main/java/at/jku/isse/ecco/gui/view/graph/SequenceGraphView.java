package at.jku.isse.ecco.gui.view.graph;

import at.jku.isse.ecco.artifact.Artifact;
import at.jku.isse.ecco.sg.SequenceGraph;
import javafx.embed.swing.SwingNode;
import javafx.scene.layout.BorderPane;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.layout.Layout;
import org.graphstream.ui.layout.springbox.implementations.SpringBox;
import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Viewer;

import javax.swing.*;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SequenceGraphView extends BorderPane {

	private Graph graph;
	private Layout layout;
	private Viewer viewer;
	private ViewPanel view;


	public SequenceGraphView() {

		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");


		this.graph = new SingleGraph("SequenceGraph");

		this.layout = new SpringBox(false);
		this.graph.addSink(layout);
		layout.addAttributeSink(this.graph);

		this.viewer = new Viewer(this.graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
		//viewer.enableAutoLayout(layout);
		this.view = viewer.addDefaultView(false); // false indicates "no JFrame"

		SwingNode swingNode = new SwingNode();

		SwingUtilities.invokeLater(() -> swingNode.setContent(view));


		this.setOnScroll(event -> view.getCamera().setViewPercent(Math.max(0.1, Math.min(1.0, view.getCamera().getViewPercent() - 0.05 * event.getDeltaY() / event.getMultiplierY()))));


		this.setCenter(swingNode);
	}

	public void showGraph(SequenceGraph sg) {
		this.viewer.disableAutoLayout();

		this.graph.removeSink(this.layout);
		this.layout.removeAttributeSink(this.graph);
		this.layout.clear();
		this.graph.clear();

		this.view.getCamera().resetView();


		this.graph.setStrict(false);

		this.graph.addAttribute("ui.quality");
		this.graph.addAttribute("ui.antialias");
		this.graph.addAttribute("ui.stylesheet",
				" node.start { fill-color: green; size: 20px; } node.end { fill-color: red; size: 20px; } ");

		this.view.getCamera().resetView();


		org.graphstream.graph.Node root = this.graph.addNode("N");
		root.setAttribute("ui.class", "start");

		this.traverseSequenceGraph(sg.getRoot(), root, "", new HashSet<>());


		this.graph.addSink(this.layout);
		this.layout.addAttributeSink(this.graph);

		this.viewer.enableAutoLayout(this.layout);
	}


	private void traverseSequenceGraph(SequenceGraph.Node sgn, org.graphstream.graph.Node parent, String currentPath, Set<Artifact<?>> nodeSet) {
		for (SequenceGraph.Transition entry : sgn.getChildren()) {
			Set<Artifact<?>> newNodeSet = new HashSet<>(nodeSet);
			newNodeSet.add(entry.getKey());

			org.graphstream.graph.Node child = this.graph.addNode("N" + this.getStringForPath(newNodeSet));
			if (entry.getValue().getChildren().isEmpty())
				child.setAttribute("ui.class", "end");

			String newPath = currentPath + "," + entry.getKey().getSequenceNumber();
			Edge edge = this.graph.addEdge("E" + newPath, parent, child, true);

			if (edge != null) {
				edge.setAttribute("label", entry.getKey() + " [" + entry.getKey().getSequenceNumber() + "]");

				this.traverseSequenceGraph(entry.getValue(), child, newPath, newNodeSet);
			}
		}

	}

	private String getStringForPath(Set<Artifact<?>> path) {
		return path.stream()
				.sorted((n1, n2) -> Integer.compare(n1.getSequenceNumber(), n2.getSequenceNumber()))
				.map(v -> String.valueOf(v.getSequenceNumber()))
				.collect(Collectors.joining(","));
	}


}
