package tests.mediaproviders;

import com.kaltura.playkit.PKMediaEntry;
import com.kaltura.playkit.plugin.connect.ResultElement;
import com.kaltura.playkit.plugin.mediaprovider.base.ErrorElement;
import com.kaltura.playkit.plugin.mediaprovider.base.OnMediaLoadCompletion;
import com.kaltura.playkit.plugin.mediaprovider.mock.MockMediaProvider;

import junit.framework.TestCase;

import org.junit.Test;

/**
 * Created by tehilarozin on 07/11/2016.
 */

public class MockProviderTest extends TestCase {

    final static String InputFile = "assets/mock/entries.playkit.json";

    @Override
    protected void setUp() throws Exception {
        super.setUp();

    }

    @Test
    public void testMockProvider() {

        final MockMediaProvider mockMediaProvider = new MockMediaProvider(InputFile, "m001");
        mockMediaProvider.load(new OnMediaLoadCompletion() {
            @Override
            public void onComplete(ResultElement<PKMediaEntry> response) {
                if (response.isSuccess()) {
                    PKMediaEntry mediaEntry = response.getResponse();
                    System.out.println("got some response. id = " + mediaEntry.getId());
                } else {
                    assertFalse(response.getError() == null);
                    System.out.println("got error on json load: " + response.getError().getMessage());
                }

                mockMediaProvider.id("1_1h1vsv3z").load(new OnMediaLoadCompletion() {
                    @Override
                    public void onComplete(ResultElement<PKMediaEntry> response) {
                        assertTrue(response.isSuccess());
                        assertTrue(response.getError() == null);
                        PKMediaEntry mediaEntry = response.getResponse();
                        assertTrue(mediaEntry.getId().equals("1_1h1vsv3z"));
                        assertTrue(mediaEntry.getSources().get(0).getId().equals("1_ude4l5pb"));

                        mockMediaProvider.id("stam").load(new OnMediaLoadCompletion() {
                            @Override
                            public void onComplete(ResultElement<PKMediaEntry> response) {
                                assertTrue(!response.isSuccess());
                                assertTrue(response.getError() != null);
                                assertTrue(response.getError().equals(ErrorElement.MediaNotFound));
                            }
                        });
                    }
                });

            }
        });
    }
}
