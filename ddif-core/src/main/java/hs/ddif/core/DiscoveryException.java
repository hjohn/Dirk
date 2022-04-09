package hs.ddif.core;

import java.util.List;
import java.util.stream.Collectors;

class DiscoveryException extends RuntimeException {
  DiscoveryException(List<String> discoveryProblems) {
    super("Problems detected during auto discovery " + discoveryProblems + ":" + discoveryProblems.stream().collect(Collectors.joining("\n - ", "\n - ", "")));
  }
}