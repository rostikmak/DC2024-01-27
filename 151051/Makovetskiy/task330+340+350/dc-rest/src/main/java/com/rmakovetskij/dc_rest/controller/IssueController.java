package com.rmakovetskij.dc_rest.controller;

import com.rmakovetskij.dc_rest.model.dto.requests.IssueRequestTo;
import com.rmakovetskij.dc_rest.model.dto.responses.IssueResponseTo;
import com.rmakovetskij.dc_rest.service.IssueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1.0/issues")
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;

    @PostMapping
    public ResponseEntity<IssueResponseTo> createIssue(@RequestBody @Valid IssueRequestTo issueRequestDto) {
        IssueResponseTo issueResponseDto = issueService.createIssue(issueRequestDto);
        return new ResponseEntity<>(issueResponseDto, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IssueResponseTo> getIssueById(@PathVariable Long id) {
        IssueResponseTo issueResponseDto = issueService.getIssueById(id);
        return new ResponseEntity<>(issueResponseDto, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<IssueResponseTo>> getAllStories() {
        List<IssueResponseTo> issueResponseDtos = issueService.getAllStories();
        return new ResponseEntity<>(issueResponseDtos, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<IssueResponseTo> updateIssue(
            @Valid
            @PathVariable Long id,
            @RequestBody IssueRequestTo issueRequestDto) {
        IssueResponseTo updatedIssueResponseDto = issueService.updateIssue(id, issueRequestDto);
        return new ResponseEntity<>(updatedIssueResponseDto, HttpStatus.OK);
    }

    @PutMapping
    public ResponseEntity<IssueResponseTo> updateIssue(@RequestBody @Valid IssueRequestTo issueRequestDto) {
        if (issueRequestDto.getId() == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        IssueResponseTo updatedIssueResponseDto = issueService.updateIssue(issueRequestDto.getId(), issueRequestDto);
        return new ResponseEntity<>(updatedIssueResponseDto, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIssue(@PathVariable Long id) {
        try {
            issueService.deleteIssue(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
