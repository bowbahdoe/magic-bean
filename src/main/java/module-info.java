/**
 * Module for the magic bean processor. This could probably be split into two modules,
 * one for the annotation and one for the processor itself, but at the moment there
 * isn't much pressing reason to do so.
 */
module dev.mccue.magicbean {
    requires java.compiler;

    provides javax.annotation.processing.Processor
            with dev.mccue.magicbean.processor.AnnotationProcessor;

    exports dev.mccue.magicbean;
    exports dev.mccue.magicbean.processor;
}
