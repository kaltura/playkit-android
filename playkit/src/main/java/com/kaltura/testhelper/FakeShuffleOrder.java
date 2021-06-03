package com.kaltura.testhelper;

import com.kaltura.android.exoplayer2.C;
import com.kaltura.android.exoplayer2.source.ShuffleOrder;

/**
 * Fake {@link ShuffleOrder} which returns a reverse order. This order is thus deterministic but
 * different from the original order.
 */
public final class FakeShuffleOrder implements ShuffleOrder {

  private final int length;

  public FakeShuffleOrder(int length) {
    this.length = length;
  }

  @Override
  public int getLength() {
    return length;
  }

  @Override
  public int getNextIndex(int index) {
    return index > 0 ? index - 1 : C.INDEX_UNSET;
  }

  @Override
  public int getPreviousIndex(int index) {
    return index < length - 1 ? index + 1 : C.INDEX_UNSET;
  }

  @Override
  public int getLastIndex() {
    return length > 0 ? 0 : C.INDEX_UNSET;
  }

  @Override
  public int getFirstIndex() {
    return length > 0 ? length - 1 : C.INDEX_UNSET;
  }

  @Override
  public ShuffleOrder cloneAndInsert(int insertionIndex, int insertionCount) {
    return new FakeShuffleOrder(length + insertionCount);
  }

  @Override
  public ShuffleOrder cloneAndRemove(int indexFrom, int indexToExclusive) {
    return new FakeShuffleOrder(length - indexToExclusive + indexFrom);
  }

  @Override
  public ShuffleOrder cloneAndClear() {
    return new FakeShuffleOrder(/* length= */ 0);
  }
}
