import org.junit.Test;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import com.breeze.graph.BrzGraph;

public class BrzGraphTest {

    @Test
    public void BrzGraphTestSuiteSetUpProperly() {
        String string = "awesome";

        assertThat(string).startsWith("awe");
        assertWithMessage("Without me, it's just aweso").that(string).contains("me");
    }

    @Test
    public void BrzGraphInstantiatedWith10NodesAndBFSApplied() {
        BrzGraph graph = BrzGraph.getInstance();
    }
}