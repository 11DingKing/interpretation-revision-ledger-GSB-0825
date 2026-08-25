package com.readingledger.service;

/**
 * 锚点摘录复核时 SHA-256 不匹配：来源文本相对登记时已发生漂移。
 */
public class AnchorHashMismatchException extends RuntimeException {

    private final String expectedSha256;
    private final String actualSha256;

    public AnchorHashMismatchException(String expectedSha256, String actualSha256) {
        super("anchor excerpt SHA-256 mismatch: source text has drifted");
        this.expectedSha256 = expectedSha256;
        this.actualSha256 = actualSha256;
    }

    public String getExpectedSha256() {
        return expectedSha256;
    }

    public String getActualSha256() {
        return actualSha256;
    }
}
