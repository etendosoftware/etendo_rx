package com.etendorx.gen.util;

import java.util.ArrayList;
import java.util.List;

public class MetadataContainer {

  private Metadata metadataMix;
  private List<Metadata> metadataList = new ArrayList<>();

  public Metadata getMetadataMix() {
    return metadataMix;
  }

  public void setMetadataMix(Metadata metadataMix) {
    this.metadataMix = metadataMix;
  }

  public List<Metadata> getMetadataList() {
    return metadataList;
  }

  public void setMetadataList(List<Metadata> metadataList) {
    this.metadataList = metadataList;
  }

}
