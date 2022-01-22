module dev.mccue.magic_bean.procesor {
    requires java.compiler;

    provides javax.annotation.processing.Processor
            with dev.mccue.magic_bean.processor.AnnotationProcessor;

    exports dev.mccue.magic_bean;
    exports dev.mccue.magic_bean.processor;
}
