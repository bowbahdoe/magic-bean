module dev.mccue.magic_bean.procesor {
    requires java.compiler;

    provides javax.annotation.processing.Processor
            with dev.mccue.magicbean.processor.AnnotationProcessor;

    exports dev.mccue.magicbean;
    exports dev.mccue.magicbean.processor;
}
