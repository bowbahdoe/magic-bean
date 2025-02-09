package dev.mccue.magicbean.models.valid;

import dev.mccue.magicbean.MagicBean;

import java.util.List;

@MagicBean
public final class Example extends ExampleBeanOps {
  int x;
  String name;
  List<String> strs;
}
