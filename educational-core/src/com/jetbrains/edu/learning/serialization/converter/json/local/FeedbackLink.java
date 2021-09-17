package com.jetbrains.edu.learning.serialization.converter.json.local;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FeedbackLink {
  @NotNull
  private LinkType myType;

  @Nullable
  private String myLink;

  public FeedbackLink() {
    myType = LinkType.STEPIK;
  }

  @NotNull
  public LinkType getType() {
    return myType;
  }

  public void setType(@NotNull LinkType type) {
    myType = type;
  }

  @Nullable
  public String getLink() {
    return myLink;
  }

  public void setLink(@Nullable String link) {
    myLink = link;
  }

  public enum LinkType {
    MARKETPLACE, STEPIK, CUSTOM, NONE
  }
}
