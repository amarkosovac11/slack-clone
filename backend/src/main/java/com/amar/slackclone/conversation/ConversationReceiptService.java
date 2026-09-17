package com.amar.slackclone.conversation;

import com.amar.slackclone.conversation.dto.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ConversationReceiptService {
    private final ConversationAccessService access; private final ConversationMessageRepository messages;
    private final ConversationParticipantRepository participants; private final ConversationMessageDeliveryRepository deliveries;
    private final SimpMessagingTemplate broker;
    public ConversationReceiptService(ConversationAccessService access,ConversationMessageRepository messages,
            ConversationParticipantRepository participants,ConversationMessageDeliveryRepository deliveries,SimpMessagingTemplate broker){
        this.access=access;this.messages=messages;this.participants=participants;this.deliveries=deliveries;this.broker=broker;
    }

    @Transactional
    public void acknowledgeDelivered(Long conversationId,Long throughMessageId,String email){
        ConversationParticipant actor=access.requireParticipant(conversationId,email);
        ConversationMessage through=requireMessage(conversationId,throughMessageId);
        List<ConversationMessage> acknowledged=deliveries.findMessagesToAcknowledge(conversationId,through.getId(),actor.getUser().getId(),actor.getJoinedAt());
        List<ConversationMessage> changed=new ArrayList<>();
        for(ConversationMessage message:acknowledged){
            if(!deliveries.existsByMessageIdAndUserId(message.getId(),actor.getUser().getId())){
                ConversationMessageDelivery delivery=new ConversationMessageDelivery();delivery.setMessage(message);delivery.setUser(actor.getUser());deliveries.save(delivery);changed.add(message);
            }
        }
        deliveries.flush();
        changed.forEach(message->publishAfterCommit(message.getSender().getId(),conversationId,
                new ConversationReceiptEvent("DELIVERED",conversationId,actor.getUser().getId(),throughMessageId,receipt(message))));
    }

    @Transactional(readOnly=true)
    public List<ConversationMessageReceiptResponse> receipts(Long conversationId,String email){
        ConversationParticipant actor=access.requireParticipant(conversationId,email);
        return messages.findAllByConversationIdAndSenderIdOrderById(conversationId,actor.getUser().getId()).stream().map(this::receipt).toList();
    }

    @Transactional(readOnly=true)
    public ConversationMessageReceiptResponse receipt(Long conversationId,Long messageId,String email){
        ConversationParticipant actor=access.requireParticipant(conversationId,email);ConversationMessage message=requireMessage(conversationId,messageId);
        if(!message.getSender().getId().equals(actor.getUser().getId()))throw new ConversationAccessDeniedException("Only the sender can inspect receipts");
        return receipt(message);
    }

    public void publishReadUpdates(ConversationParticipant reader,Long throughMessageId){
        if(throughMessageId==null)return;
        Long conversationId=reader.getConversation().getId();
        messages.findAllByConversationIdAndIdLessThanEqualAndSenderIdNotOrderById(conversationId,throughMessageId,reader.getUser().getId())
                .stream().filter(message->!message.getCreatedAt().isBefore(reader.getJoinedAt())).forEach(message->
                    publishAfterCommit(message.getSender().getId(),conversationId,new ConversationReceiptEvent("READ",conversationId,
                            reader.getUser().getId(),throughMessageId,receipt(message))));
    }

    private ConversationMessageReceiptResponse receipt(ConversationMessage message){
        List<ConversationParticipant> eligible=participants.findAllByConversationIdAndLeftAtIsNullOrderByJoinedAt(message.getConversation().getId()).stream()
                .filter(p->!p.getUser().getId().equals(message.getSender().getId())&&!p.getJoinedAt().isAfter(message.getCreatedAt())).toList();
        Map<Long,ConversationMessageDelivery> delivered=deliveries.findAllByMessageId(message.getId()).stream()
                .collect(Collectors.toMap(d->d.getUser().getId(),Function.identity()));
        List<ConversationReceiptRecipientResponse> recipients=eligible.stream().map(p->{
            var delivery=delivered.get(p.getUser().getId());boolean read=p.getLastReadMessage()!=null&&p.getLastReadMessage().getId()>=message.getId();
            return new ConversationReceiptRecipientResponse(p.getUser().getId(),p.getUser().getDisplayName(),delivery==null?null:delivery.getDeliveredAt(),read);
        }).toList();
        return new ConversationMessageReceiptResponse(message.getId(),(int)recipients.stream().filter(r->r.deliveredAt()!=null).count(),
                (int)recipients.stream().filter(ConversationReceiptRecipientResponse::read).count(),recipients.size(),recipients);
    }
    private ConversationMessage requireMessage(Long conversationId,Long messageId){return messages.findByIdAndConversationId(messageId,conversationId)
            .orElseThrow(()->new ConversationValidationException("Message does not belong to this conversation"));}
    private void publishAfterCommit(Long userId,Long conversationId,ConversationReceiptEvent event){
        Runnable publish=()->broker.convertAndSend("/topic/users/"+userId+"/conversations/"+conversationId+"/receipts",event);
        if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCommit(){publish.run();}});else publish.run();
    }
}
