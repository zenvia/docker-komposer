import com.zenvia.komposer.junit.KomposerRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * Created by tiago on 05/06/15.
 */
public class TestWithJUnit {

    @Rule public KomposerRule komposer = new KomposerRule("src/test/resources/docker-compose.yml");

    @Test
    public void testSomething() {

    }
}
