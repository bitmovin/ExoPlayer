package com.google.android.exoplayer.dash.mpd;

import com.google.android.exoplayer.util.Assertions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Builds a list of {@link ContentProtection} elements for an {@link AdaptationSet}.
 * <p>
 * If child Representation elements contain ContentProtection elements, then it is required that
 * they all define the same ones. If they do, the ContentProtection elements are bubbled up to the
 * AdaptationSet. Child Representation elements defining different ContentProtection elements is
 * considered an error.
 */
public final class ContentProtectionsBuilder implements Comparator<ContentProtection> {

    private ArrayList<ContentProtection> adaptationSetProtections;
    private ArrayList<ContentProtection> representationProtections;
    private ArrayList<ContentProtection> currentRepresentationProtections;

    private boolean representationProtectionsSet;

    /**
     * Adds a {@link ContentProtection} found in the AdaptationSet element.
     *
     * @param contentProtection The {@link ContentProtection} to add.
     */
    public void addAdaptationSetProtection(ContentProtection contentProtection) {
        if (adaptationSetProtections == null) {
            adaptationSetProtections = new ArrayList<>();
        }
        maybeAddContentProtection(adaptationSetProtections, contentProtection);
    }

    /**
     * Adds a {@link ContentProtection} found in a child Representation element.
     *
     * @param contentProtection The {@link ContentProtection} to add.
     */
    public void addRepresentationProtection(ContentProtection contentProtection) {
        if (currentRepresentationProtections == null) {
            currentRepresentationProtections = new ArrayList<>();
        }
        maybeAddContentProtection(currentRepresentationProtections, contentProtection);
    }

    /**
     * Should be invoked after processing each child Representation element, in order to apply
     * consistency checks.
     */
    public void endRepresentation() {
        if (!representationProtectionsSet) {
            if (currentRepresentationProtections != null) {
                Collections.sort(currentRepresentationProtections, this);
            }
            representationProtections = currentRepresentationProtections;
            representationProtectionsSet = true;
        } else {
            // Assert that each Representation element defines the same ContentProtection elements.
            if (currentRepresentationProtections == null) {
                Assertions.checkState(representationProtections == null);
            } else {
                Collections.sort(currentRepresentationProtections, this);
                Assertions.checkState(currentRepresentationProtections.equals(representationProtections));
            }
        }
        currentRepresentationProtections = null;
    }

    /**
     * Returns the final list of consistent {@link ContentProtection} elements.
     */
    public ArrayList<ContentProtection> build() {
        if (adaptationSetProtections == null) {
            return representationProtections;
        } else if (representationProtections == null) {
            return adaptationSetProtections;
        } else {
            // Bubble up ContentProtection elements found in the child Representation elements.
            for (int i = 0; i < representationProtections.size(); i++) {
                maybeAddContentProtection(adaptationSetProtections, representationProtections.get(i));
            }
            return adaptationSetProtections;
        }
    }

    /**
     * Checks a ContentProtection for consistency with the given list, adding it if necessary.
     * <ul>
     * <li>If the new ContentProtection matches another in the list, it's consistent and is not
     *     added to the list.
     * <li>If the new ContentProtection has the same schemeUriId as another ContentProtection in the
     *     list, but its other attributes do not match, then it's inconsistent and an
     *     {@link IllegalStateException} is thrown.
     * <li>Else the new ContentProtection has a unique schemeUriId, it's consistent and is added.
     * </ul>
     *
     * @param contentProtections The list of ContentProtection elements currently known.
     * @param contentProtection The ContentProtection to add.
     */
    private void maybeAddContentProtection(List<ContentProtection> contentProtections,
                                           ContentProtection contentProtection) {
        if (!contentProtections.contains(contentProtection)) {
            for (int i = 0; i < contentProtections.size(); i++) {
                // If contains returned false (no complete match), but find a matching schemeUriId, then
                // the MPD contains inconsistent ContentProtection data.
                Assertions.checkState(
                        !contentProtections.get(i).schemeUriId.equals(contentProtection.schemeUriId));
            }
            contentProtections.add(contentProtection);
        }
    }

    // Comparator implementation.

    @Override
    public int compare(ContentProtection first, ContentProtection second) {
        return first.schemeUriId.compareTo(second.schemeUriId);
    }

}
