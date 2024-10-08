package io.cequence.openaiscala.examples

import io.cequence.openaiscala.domain.AssistantTool.FunctionTool
import io.cequence.openaiscala.domain.{AssistantToolResource, ModelId}

import scala.concurrent.Future

object CreateAssistant extends Example {
  override protected def run: Future[_] =
    for {
      assistant <- service.createAssistant(
        model = ModelId.gpt_4o_mini,
        name = Some("Math Tutor"),
        instructions = Some(
          "You are a personal math tutor. When asked a question, write and run Python code to answer the question."
        ),
        tools = Seq(
          FunctionTool("name", description = None, parameters = Map())
        ),
        toolResources = Some(AssistantToolResource())
      )
    } yield println(assistant)
}
