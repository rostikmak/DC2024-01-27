package by.bsuir.publisher.services.impl;

import by.bsuir.publisher.dto.MessageActionDto;
import by.bsuir.publisher.dto.MessageActionTypeDto;
import by.bsuir.publisher.dto.requests.NoteRequestDto;
import by.bsuir.publisher.dto.responses.ErrorDto;
import by.bsuir.publisher.dto.responses.NoteResponseDto;
import by.bsuir.publisher.dto.responses.converters.NoteResponseConverter;
import by.bsuir.publisher.exceptions.ServiceException;
import by.bsuir.publisher.repositories.NoteCacheRepository;
import by.bsuir.publisher.services.NoteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Validated
@Transactional(rollbackFor = {ServiceException.class})
public class NoteServiceImpl implements NoteService {
    private final ObjectMapper objectMapper;
    private final NoteResponseConverter noteResponseConverter;
    private final NoteCacheRepository noteCacheRepository;

    private final ReplyingKafkaTemplate<String, MessageActionDto, MessageActionDto> replyingKafkaTemplate;

    @Value("${topic.inTopic}")
    private String inTopic;

    @Value("${topic.outTopic}")
    private String outTopic;

    @KafkaListener(topics = "${topic.messageChangeTopic}")
    protected void receiveMessageChange(MessageActionDto messageActionDto) {
        switch (messageActionDto.getAction()) {
            case CREATE, UPDATE -> {
                NoteResponseDto noteResponseDto = objectMapper.
                        convertValue(messageActionDto.getData(), NoteResponseDto.class);
                noteCacheRepository.save(noteResponseConverter.fromDto(noteResponseDto));
            }
            case DELETE -> {
                Long id = Long.valueOf((String) messageActionDto.getData());
                noteCacheRepository.deleteById(id);
            }
        }
    }

    protected MessageActionDto sendMessageAction(MessageActionDto messageActionDto) {
        ProducerRecord<String, MessageActionDto> record = new ProducerRecord<>(inTopic, null,
                System.currentTimeMillis(), String.valueOf(messageActionDto.toString()),
                messageActionDto);
        record.headers().add(new RecordHeader(KafkaHeaders.REPLY_TOPIC, outTopic.getBytes()));
        RequestReplyFuture<String, MessageActionDto, MessageActionDto> response = replyingKafkaTemplate.sendAndReceive(record);
        return response.orTimeout(1000, TimeUnit.MILLISECONDS).join().value();
    }

    @Override
    public NoteResponseDto create(@NonNull NoteRequestDto dto) throws ServiceException {
        //Synchronous, as response in tests shall contain created message id, and if passed id is null,
        //asynchronous messaging is not appropriate(we won't be able to obtain generated id)
        MessageActionDto action = sendMessageAction(MessageActionDto.builder().
                action(MessageActionTypeDto.CREATE).
                data(dto).
                build());
        NoteResponseDto response = objectMapper.convertValue(action.getData(), NoteResponseDto.class);
        if (ObjectUtils.allNull(response.getId(), response.getTweetId(), response.getContent())) {
            throw new ServiceException(objectMapper.convertValue(action.getData(), ErrorDto.class));
        }
        noteCacheRepository.save(noteResponseConverter.fromDto(response));
        return response;
    }

    @Override
    public Optional<NoteResponseDto> read(@NonNull Long uuid) {
        return Optional.ofNullable(noteCacheRepository.findById(uuid).map(noteResponseConverter::toDto).orElseGet(() -> {
            MessageActionDto action = sendMessageAction(MessageActionDto.builder().
                    action(MessageActionTypeDto.READ).
                    data(String.valueOf(uuid)).
                    build());
            return objectMapper.convertValue(action.getData(), NoteResponseDto.class);
        }));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<NoteResponseDto> readAll() {
        return (List<NoteResponseDto>) ((List)sendMessageAction(MessageActionDto.builder().
                action(MessageActionTypeDto.READ_ALL).
                build()).getData()).stream().map(v -> objectMapper.convertValue(v, NoteResponseDto.class)).toList();
    }

    @Override
    public NoteResponseDto update(@NonNull NoteRequestDto dto) throws ServiceException {
        MessageActionDto action = sendMessageAction(MessageActionDto.builder().
                action(MessageActionTypeDto.UPDATE).
                data(dto).
                build());
       NoteResponseDto response = objectMapper.convertValue(action.getData(), NoteResponseDto.class);
       if (ObjectUtils.allNull(response.getId(), response.getContent(), response.getTweetId())) {
           throw new ServiceException(objectMapper.convertValue(action.getData(), ErrorDto.class));
       }
       noteCacheRepository.save(noteResponseConverter.fromDto(response));
       return response;
    }

    @Override
    public Long delete(@NonNull Long uuid) throws ServiceException {
        MessageActionDto action = sendMessageAction(MessageActionDto.builder().
                action(MessageActionTypeDto.DELETE).
                data(String.valueOf(uuid)).
                build());
        try {
            noteCacheRepository.deleteById(uuid);
            return Long.valueOf((Integer) action.getData());
        } catch (ClassCastException e) {
            throw new ServiceException(objectMapper.convertValue(action.getData(), ErrorDto.class));
        }
    }
}
