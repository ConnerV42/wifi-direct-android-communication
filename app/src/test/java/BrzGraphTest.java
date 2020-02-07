import com.breeze.datatypes.BrzNode;

import org.junit.Test;
import org.junit.jupiter.api.AfterEach;


import java.util.List;
import java.util.Random;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

public class BrzGraphTest { // truth docs: https://truth.dev/api/1.0.1/

    public TBrzGraph graph;

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
        this.graph = TBrzGraph.getInstance();

        BrzNode startNode = null, endNode = null;
        Random r = new Random();
        int index1 = r.nextInt(14);
        int index2;

        do {
            index2 = r.nextInt(14);
        } while (index1 == index2);

        BrzNode prev = null;
        for(int i = 0; i < 13; i++) {
            BrzNode node = new BrzNode();
            node.generateID();

            this.graph.addVertex(node);

            if (i != 0) {
                System.out.println();
                this.graph.addEdge(prev.id, node.id);
                prev = node;
            } else {
                prev = node;
            }

            if (i == index1) {
                startNode = node;
                this.graph.hostNode = node;
            }
            if (i == index2) endNode = node;
        }

        System.out.println(
                "Start Node: " + startNode.id + "\n" + "End Node: " + endNode.id);
        List<String> list = this.graph.bfs(startNode.id, endNode.id);
        System.out.println(list + "\n\nTo String..." + this.graph.toString());
        assertThat(list).isNotNull();
        assertThat(list.get(0)).isEqualTo(startNode.id);
        assertThat(list.get(list.size() - 1)).isEqualTo(endNode.id);
    }
}