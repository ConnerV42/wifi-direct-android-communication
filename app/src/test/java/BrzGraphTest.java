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
    public BrzGraph graph;

    @BeforeEach
    public void init() {
        this.graph = BrzGraph.getInstance();
    }

    @AfterEach
    public void log() {
        System.out.println(this.graph.toString());
    }

    @Test
    public void BrzGraphTestSuiteSetUpProperly() {
        String string = "awesome";

        assertThat(string).startsWith("awe");
        assertWithMessage("Without me, it's just aweso").that(string).contains("me");
    }

    @Test
    public void BrzGraphAdd10Verticeswith9Edges_StraightLine() {
        BrzNode startNode = null, endNode = null;
        Random r = new Random();
        int index1 = r.nextInt(14);
        int index2;

        do {
            index2 = r.nextInt(14);
        } while (index1 != index2);

        BrzNode prev = null;
        for(int i = 0; i < 13; i++) {
            BrzNode node = new BrzNode();
            node.generateID();

            // mock out the emit() calls so it doesn't crash
            this.graph.addVertex(node);

            if (i != 0) {
                this.graph.addEdge(prev.id, node.id);
                prev = node;
            } else {
                prev = node;
            }

            if (i == index1) startNode = node;
            if (i == index2) endNode = node;
        }

        List<String> list = this.graph.bfs(startNode.id, endNode.id);
        assertThat(list).isNotNull();
    }
}