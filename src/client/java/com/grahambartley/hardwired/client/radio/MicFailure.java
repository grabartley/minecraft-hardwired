package com.grahambartley.hardwired.client.radio;

public final class MicFailure extends Exception {

  private final MicProblem problem;

  public MicFailure(MicProblem problem, Throwable cause) {
    super(problem.name(), cause);
    this.problem = problem;
  }

  public MicProblem problem() {
    return problem;
  }
}
