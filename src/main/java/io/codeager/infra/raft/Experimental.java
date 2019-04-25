package io.codeager.infra.raft;

/**
 * @author Jiupeng Zhang
 * @since 04/25/2019
 */
public @interface Experimental {
    enum Statement {
        NOT_FULLY_DESIGNED, NOT_FULLY_CODED, TODO_TEST, MALFUNCTIONED, TODO
    }

    Statement[] value();

    Class ref() default Experimental.class;

    String note() default "";
}
