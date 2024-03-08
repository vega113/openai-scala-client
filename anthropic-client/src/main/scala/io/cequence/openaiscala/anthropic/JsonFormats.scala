package io.cequence.openaiscala.anthropic

import io.cequence.openaiscala.JsonUtil
import io.cequence.openaiscala.anthropic.domain.Message.{
  AssistantMessage,
  AssistantMessageContent,
  UserMessage,
  UserMessageContent
}
import io.cequence.openaiscala.anthropic.domain.Content.ContentBlock.TextBlock
import io.cequence.openaiscala.anthropic.domain.Content.{
  ContentBlock,
  ContentBlocks,
  SingleString
}
import io.cequence.openaiscala.anthropic.domain.{Message, ChatRole, Content}
import io.cequence.openaiscala.anthropic.service.response.CreateMessageResponse
import io.cequence.openaiscala.anthropic.service.response.CreateMessageResponse.UsageInfo
import play.api.libs.functional.syntax._
import play.api.libs.json._

object JsonFormats extends JsonFormats

trait JsonFormats {

  implicit lazy val chatRoleFormat: Format[ChatRole] =
    JsonUtil.enumFormat[ChatRole](ChatRole.allValues: _*)
  implicit lazy val usageInfoFormat: Format[UsageInfo] = Json.format[UsageInfo]

  implicit lazy val userMessageFormat: Format[UserMessage] = Json.format[UserMessage]
  implicit lazy val userMessageContentFormat: Format[UserMessageContent] =
    Json.format[UserMessageContent]
  implicit lazy val assistantMessageFormat: Format[AssistantMessage] =
    Json.format[AssistantMessage]
  implicit lazy val assistantMessageContentFormat: Format[AssistantMessageContent] =
    Json.format[AssistantMessageContent]

  implicit lazy val textBlockFormat: Format[TextBlock] = Json.format[TextBlock]

  implicit lazy val contentBlocksFormat: Format[ContentBlocks] = Json.format[ContentBlocks]

  implicit val textBlockWrites: Writes[TextBlock] = Json.writes[TextBlock]
  implicit val textBlockReads: Reads[TextBlock] = Json.reads[TextBlock]

  implicit val contentBlockWrites: Writes[ContentBlock] = new Writes[ContentBlock] {
    def writes(block: ContentBlock): JsValue = block match {
      case tb: TextBlock =>
        Json.obj("type" -> "text") ++ Json.toJson(tb)(textBlockWrites).as[JsObject]
    }
  }

  implicit val contentBlockReads: Reads[ContentBlock] = (
    (__ \ "type").read[String] and
      (__ \ "text").readNullable[String]
  ).tupled.flatMap {
    case ("text", Some(text)) => Reads.pure(TextBlock(text))
    case _                    => Reads(_ => JsError("Unsupported or invalid content block"))
  }

  implicit val contentReads: Reads[Content] = new Reads[Content] {
    def reads(json: JsValue): JsResult[Content] = json match {
      case JsString(str) => JsSuccess(SingleString(str))
      case JsArray(arr)  => Json.fromJson[Seq[ContentBlock]](json).map(ContentBlocks(_))
      case _             => JsError("Invalid content format")
    }
  }

  implicit val baseMessageWrites: Writes[Message] = new Writes[Message] {
    def writes(message: Message): JsValue = message match {
      case UserMessage(content) => Json.obj("role" -> "user", "content" -> content)
      case UserMessageContent(content) =>
        Json.obj(
          "role" -> "user",
          "content" -> content.map(Json.toJson(_)(contentBlockWrites))
        )
      case AssistantMessage(content) => Json.obj("role" -> "assistant", "content" -> content)
      case AssistantMessageContent(content) =>
        Json.obj(
          "role" -> "assistant",
          "content" -> content.map(Json.toJson(_)(contentBlockWrites))
        )
      // Add cases for other subclasses if necessary
    }
  }

  implicit val baseMessageReads: Reads[Message] = (
    (__ \ "role").read[String] and
      (__ \ "content").lazyRead(contentReads)
  ).tupled.flatMap {
    case ("user", SingleString(text))         => Reads.pure(UserMessage(text))
    case ("user", ContentBlocks(blocks))      => Reads.pure(UserMessageContent(blocks))
    case ("assistant", SingleString(text))    => Reads.pure(AssistantMessage(text))
    case ("assistant", ContentBlocks(blocks)) => Reads.pure(AssistantMessageContent(blocks))
    case _ => Reads(_ => JsError("Unsupported role or content type"))
  }

  implicit val createMessageResponseWrites: Writes[CreateMessageResponse] = (
    (__ \ "id").write[String] and
      (__ \ "content")
        .write[Seq[ContentBlock]](Writes.seq(contentBlockWrites))
        .contramap[ContentBlocks](_.blocks) and
      (__ \ "model").write[String] and
      (__ \ "stop_reason").writeNullable[String] and
      (__ \ "stop_sequence").writeNullable[String] and
      (__ \ "usage").write[UsageInfo]
  )(unlift(CreateMessageResponse.unapply))

  implicit val createMessageResponseReads: Reads[CreateMessageResponse] = (
    (__ \ "id").read[String] and
      (__ \ "content").read[Seq[ContentBlock]].map(ContentBlocks(_)) and
      (__ \ "model").read[String] and
      (__ \ "stop_reason").readNullable[String] and
      (__ \ "stop_sequence").readNullable[String] and
      (__ \ "usage").read[UsageInfo]
  )(CreateMessageResponse.apply _)

}
