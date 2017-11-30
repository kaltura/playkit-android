package com.kaltura.playkit.player;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.RendererCapabilities;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;

public class MultiAudioTrackSelector extends DefaultTrackSelector
{
    public MultiAudioTrackSelector( TrackSelection.Factory adaptiveVideoTrackSelectionFactory )
    {
        super( adaptiveVideoTrackSelectionFactory );
    }

    @Override
    protected TrackSelection[] selectTracks(RendererCapabilities[] rendererCapabilities, TrackGroupArray[] rendererTrackGroupArrays, int[][][] rendererFormatSupports) throws ExoPlaybackException
    {
        // Make a track selection for each renderer.
        int rendererCount = rendererCapabilities.length;
        Parameters params = getParameters();

        // Call parent to fill in video renderer
        TrackSelection[] rendererTrackSelections = super.selectTracks( rendererCapabilities, rendererTrackGroupArrays, rendererFormatSupports );

        boolean bSeenAudio = false;
        for (int i = 0; i < rendererCount; i++)
        {
            if( rendererCapabilities[ i ].getTrackType() == C.TRACK_TYPE_AUDIO && rendererTrackSelections[ i ] != null )
            {
                if( bSeenAudio )
                {
                    rendererTrackSelections[ i ] = null;
                }
                bSeenAudio = true;
            }
        }

        return rendererTrackSelections;
    }
}