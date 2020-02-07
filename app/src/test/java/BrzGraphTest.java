import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.breeze.graph.BrzGraph;
import com.breeze.datatypes.BrzNode;

import java.util.List;
import java.util.Random;

public class BrzGraphTest { // truth docs: https://truth.dev/api/1.0.1/
    private BrzGraph graph = new BrzGraph();

    @BeforeEach
    public void init() {
        this.graph = new BrzGraph();
    }

    @AfterEach
    public void log() {
        System.out.println(this.graph.toString());
    }

    @Test
    public void TestSuiteSetUpProperly() {
        String string = "awesome";

        assertThat(string).startsWith("awe");
        assertWithMessage("Without me, it's just aweso").that(string).contains("me");
    }

    @Test
    public void Add10Verticeswith9Edges_StraightLine() {
        BrzNode prev = null;
        for (int i = 0; i < 10; i++) {
            BrzNode node = new BrzNode();
            node.id = "" + i;

            this.graph.addVertex(node);
            if (prev != null)
                this.graph.addEdge(prev.id, node.id);

            prev = node;
        }

        List<String> list = this.graph.bfs("0", "9");
        assertThat(list).isNotNull();
        assertThat(list).containsExactlyElementsIn(new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"});
    }

    @Test
    public void Add10Vertices_StraigtLine_Remove5() {
        BrzNode prev = null;
        for (int i = 0; i < 10; i++) {
            BrzNode node = new BrzNode();
            node.id = "" + i;

            this.graph.addVertex(node);
            if (prev != null)
                this.graph.addEdge(prev.id, node.id);

            prev = node;
        }

        graph.removeVertex("5");
        graph.removeDisconnected("0");

        assertThat(graph.getSize()).isEqualTo(5);
        assertThat(graph.getVertexIds()).containsExactlyElementsIn(new String[]{"0", "1", "2", "3", "4"});
        assertThat(graph.getEdgeIds()).containsExactlyElementsIn(new String[]{"0", "1", "2", "3", "4"});

        List<String> list = this.graph.bfs("0", "4");
        assertThat(list).isNotNull();
        assertThat(list).containsExactlyElementsIn(new String[]{"0", "1", "2", "3", "4"});

        list = this.graph.bfs("0", "9");
        assertThat(list).isNull();
    }

    @Test
    public void Add5Vertices_Remove1() {
        for (int i = 0; i < 5; i++) {
            BrzNode node = new BrzNode();
            node.id = "" + i;
            graph.addVertex(node);
        }

        graph.addEdge("0", "1");
        graph.addEdge("1", "4");

        graph.addEdge("0", "2");
        graph.addEdge("2", "3");
        graph.addEdge("3", "4");

        List<String> list = this.graph.bfs("0", "4");
        assertThat(list).isNotNull();
        assertThat(list).containsExactlyElementsIn(new String[]{"0", "1", "4"});

        graph.removeVertex("1");
        graph.removeDisconnected("0");

        list = this.graph.bfs("0", "4");
        assertThat(list).isNotNull();
        assertThat(list).containsExactlyElementsIn(new String[]{"0", "2", "3", "4"});

        assertThat(graph.getSize()).isEqualTo(4);
        assertThat(graph.getVertexIds()).containsExactlyElementsIn(new String[]{"0", "2", "3", "4"});
        assertThat(graph.getEdgeIds()).containsExactlyElementsIn(new String[]{"0", "2", "3", "4"});
    }

    @Test
    public void Merge2Graphs_5VerticesEach() {
        BrzGraph graph2 = new BrzGraph();

        for (int i = 0; i < 5; i++) {
            BrzNode node = new BrzNode();
            node.id = "" + i;
            graph.addVertex(node);
        }

        for (int i = 5; i < 10; i++) {
            BrzNode node = new BrzNode();
            node.id = "" + i;
            graph2.addVertex(node);
        }

        graph.addEdge("0", "1");
        graph.addEdge("0", "2");
        graph.addEdge("1", "4");
        graph.addEdge("2", "3");
        graph.addEdge("3", "4");

        graph2.addEdge("5", "6");
        graph2.addEdge("5", "8");
        graph2.addEdge("6", "7");
        graph2.addEdge("7", "8");
        graph2.addEdge("8", "9");

        // Connect the graphs on 4 -> 5
        graph2.addVertex(graph.getVertex("4"));
        graph2.addEdge("4", "5");

        // Merge graph2 into graph
        graph.mergeGraph(graph2.toJSON());

        assertThat(graph.getSize()).isEqualTo(10);
        assertThat(graph.getVertexIds()).containsExactlyElementsIn(new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"});
        assertThat(graph.getEdgeIds()).containsExactlyElementsIn(new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"});

        List<String> list = this.graph.bfs("0", "9");
        assertThat(list).isNotNull();
        assertThat(list).containsExactlyElementsIn(new String[]{"0", "1", "4", "5", "8", "9"});
    }
}