package com.coliving.api.messaging.application

import com.coliving.api.messaging.application.dto.OpenConversationCommand
import com.coliving.api.messaging.application.dto.ReportConversationCommand
import com.coliving.api.messaging.application.dto.SendMessageCommand
import com.coliving.api.messaging.application.usecase.BlockService
import com.coliving.api.messaging.application.usecase.ConversationService
import com.coliving.api.messaging.application.usecase.MessageService
import com.coliving.api.messaging.application.usecase.ReportService
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.InvalidArgumentException
import com.coliving.api.shared.error.NotFoundException
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Blocks (RF-053) and conversation reports (RF-052) of messaging. */
class MessagingSafetyTest {

    private val conversations = FakeConversationRepository()
    private val messages = FakeMessageRepository()
    private val notifications = FakeNotificationRepository()
    private val blocks = FakeUserBlockRepository()
    private val reports = FakeConversationReportRepository()
    private val reportCases = FakeReportCasePort()
    private val parties = FakeReservationPartiesPort()
    private val conversationService = ConversationService(conversations, messages, parties)
    private val messageService = MessageService(conversations, messages, notifications, conversationService, blocks)
    private val blockService = BlockService(blocks)
    private val reportService = ReportService(conversations, reports, reportCases)

    private val guestId = UUID.randomUUID()
    private val hostId = UUID.randomUUID()

    private fun openConversation(): UUID {
        val reservationId = UUID.randomUUID()
        parties.add(reservationId = reservationId, guestUserId = guestId, hostUserId = hostId)
        return conversationService.open(OpenConversationCommand(reservationId, guestId)).conversation.id
    }

    @Test
    fun `a block stops the messages in both directions (RF-053)`() {
        val conversationId = openConversation()
        blockService.block(guestId, hostId)

        assertFailsWith<ForbiddenException> {
            messageService.send(SendMessageCommand(conversationId, guestId, "Hola"))
        }
        assertFailsWith<ForbiddenException> {
            messageService.send(SendMessageCommand(conversationId, hostId, "Hola"))
        }
        assertTrue { blocks.existsBetween(guestId, hostId) }
    }

    @Test
    fun `unblocking restores the conversation`() {
        val conversationId = openConversation()
        blockService.block(guestId, hostId)
        blockService.unblock(guestId, hostId)

        val view = messageService.send(SendMessageCommand(conversationId, guestId, "Hola de nuevo"))
        assertEquals(guestId, view.senderUserId)
        assertFailsWith<NotFoundException> { blockService.unblock(guestId, hostId) }
    }

    @Test
    fun `nobody blocks themselves and active blocks cannot repeat`() {
        assertFailsWith<InvalidArgumentException> { blockService.block(guestId, guestId) }
        blockService.block(guestId, hostId)
        assertFailsWith<ConflictException> { blockService.block(guestId, hostId) }
        assertEquals(1, blockService.listMine(guestId).size)
        assertTrue { blockService.listMine(hostId).isEmpty() }
    }

    @Test
    fun `reporting opens a moderation case and stores its reference (RF-052)`() {
        val conversationId = openConversation()

        val report = reportService.report(
            ReportConversationCommand(conversationId, guestId, "Mensajes abusivos del anfitrión"),
        )

        assertEquals(1, reportCases.openedCases.size)
        assertEquals(reportCases.openedCases.single(), report.caseId)
        assertEquals(hostId, report.reportedUserId)
        assertEquals(conversationId, report.conversationId)
        assertEquals(1, reportService.listMine(guestId).size)
    }

    @Test
    fun `only one report per conversation and only a party can report`() {
        val conversationId = openConversation()
        reportService.report(ReportConversationCommand(conversationId, guestId, "Primero"))

        assertFailsWith<ConflictException> {
            reportService.report(ReportConversationCommand(conversationId, guestId, "Otra vez"))
        }
        assertFailsWith<ForbiddenException> {
            reportService.report(ReportConversationCommand(conversationId, UUID.randomUUID(), "No soy parte"))
        }
        // The reason is validated before the uniqueness check (fresh conversation).
        val otherConversation = openConversation()
        assertFailsWith<InvalidArgumentException> {
            reportService.report(ReportConversationCommand(otherConversation, guestId, "   "))
        }
    }

    @Test
    fun `an unknown conversation cannot be reported`() {
        assertFailsWith<NotFoundException> {
            reportService.report(ReportConversationCommand(UUID.randomUUID(), guestId, "Nada"))
        }
    }
}