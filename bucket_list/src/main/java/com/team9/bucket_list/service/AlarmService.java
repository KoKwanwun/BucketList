package com.team9.bucket_list.service;

import com.team9.bucket_list.domain.dto.alarm.AlarmListResponse;
import com.team9.bucket_list.domain.entity.Alarm;
import com.team9.bucket_list.domain.entity.Member;
import com.team9.bucket_list.domain.entity.Post;
import com.team9.bucket_list.execption.ApplicationException;
import com.team9.bucket_list.execption.ErrorCode;
import com.team9.bucket_list.repository.AlarmRepository;
import com.team9.bucket_list.repository.MemberRepository;
import com.team9.bucket_list.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@EnableAsync
@Transactional(readOnly = true)
@Slf4j
public class AlarmService {

    private final AlarmRepository alarmRepository;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;

    public Page<AlarmListResponse> alarmList(Pageable pageable, Long memberId) {
        return AlarmListResponse.toList(alarmRepository.findAllByMember_IdAndReadStatusOrderByIdDesc(memberId,(byte)0,pageable));
    }

    public Page<AlarmListResponse> newAlarmList(Pageable pageable, Long memberId, Long id) {
        return AlarmListResponse.toList(alarmRepository.findAllByMember_IdAndReadStatusAndIdGreaterThanOrderByIdDesc(pageable,memberId,(byte)0,id));
    }

    public Page<AlarmListResponse> newAlarmListScroll(Pageable pageable, Long memberId, Long id) {
        return AlarmListResponse.toList(alarmRepository.findAllByMember_IdAndReadStatusAndIdLessThanOrderByIdDesc(pageable,memberId,(byte)0,id));
    }

    //sender : 댓글이나 좋아요, 신청서를 작성한 사람, 알람을 보내는 사람의 아이디
    //postId : 해당 포스트
    //카테고리 :  0:댓글, 1:좋아요, 2:참가자가 신청서 작성, 3:신청서 승낙, 4:멤버 리뷰, 5:버킷리스트 리뷰, 6:기타
    /*
        멤버리뷰 : senderId(리뷰 요청하는사람), postId(리뷰를 하는사람)
        버킷리뷰 : senderId(리뷰 하는사람), postId(포스트 아이디)

    * */
//    @Async
    @Transactional
    public void sendAlarm2(Long senderId, Long receiverId, Long postId, byte category) {
        if (senderId != receiverId) {
            Member sender = memberRepository.findById(senderId).orElseThrow(() -> new ApplicationException(ErrorCode.USERNAME_NOT_FOUNDED));
            Member receiver = memberRepository.findById(receiverId).orElseThrow(() -> new ApplicationException(ErrorCode.USERNAME_NOT_FOUNDED));
            Post post = postRepository.findById(postId).orElseThrow( () -> new ApplicationException(ErrorCode.POST_NOT_FOUND));
            alarmRepository.save(Alarm.save(category, receiver, post.getId(), post.getTitle(), sender.getUserName()));
        }
    }

    @Transactional
    public void sendMemberReviewAlarm(Long senderId, Long receiverId, byte category) {
        if (senderId != receiverId) {
            Member sender = memberRepository.findById(senderId).orElseThrow(() -> new ApplicationException(ErrorCode.USERNAME_NOT_FOUNDED));
            Member receiver = memberRepository.findById(receiverId).orElseThrow(() -> new ApplicationException(ErrorCode.USERNAME_NOT_FOUNDED));
            alarmRepository.save(Alarm.save(category, receiver, null, null, sender.getUserName()));
        }
    }

    @Transactional
    public void sendBucketListReviewAlarm(Long receiverId, Long postId, byte category) {
        Member receiver = memberRepository.findById(receiverId).orElseThrow(() -> new ApplicationException(ErrorCode.USERNAME_NOT_FOUNDED));
        Post post = postRepository.findById(postId).orElseThrow( () -> new ApplicationException(ErrorCode.POST_NOT_FOUND));
        alarmRepository.save(Alarm.save(category, receiver, post.getId(), post.getTitle(), post.getMember().getUserName()));
    }


    @Transactional
    public void sendAlarm(Long senderId, Long getterId, byte category) {
        Post post = null;
        Member sender = null;
        Member receiver = null;


        if(category != (byte) 4)
            post = postRepository.findById(getterId).orElseThrow( () -> new ApplicationException(ErrorCode.POST_NOT_FOUND));
        else {
            receiver = memberRepository.findById(getterId).orElseThrow( () -> new ApplicationException(ErrorCode.USERNAME_NOT_FOUNDED));
        }
        if(category != (byte) 5)
            sender = memberRepository.findById(senderId).orElseThrow( () -> new ApplicationException(ErrorCode.USERNAME_NOT_FOUNDED));
        else
            receiver = memberRepository.findById(senderId).orElseThrow( () -> new ApplicationException(ErrorCode.USERNAME_NOT_FOUNDED));


        if(category == (byte) 4) {
            Optional<Alarm> optionalAlarm = alarmRepository.findBySenderNameAndMemberIdAndCategory(sender.getUserName(), getterId, category);
            if(optionalAlarm.isEmpty()){
                alarmRepository.save(Alarm.save(category, receiver, null, null, sender.getUserName()));
            }
        }
        else if(category == (byte) 5) {
            Optional<Alarm> optionalAlarm = alarmRepository.findByMemberIdAndPostIdAndCategory(senderId, getterId, category);
            if(optionalAlarm.isEmpty()) {
                alarmRepository.save(Alarm.save(category, receiver, post.getId(), post.getTitle(), post.getMember().getUserName()));
            }
        }
        else
            alarmRepository.save(Alarm.save(category,post.getMember(),post.getId(), post.getTitle(),sender.getUserName()));
    }


    public int alarmCount(Long memberId) {
        return alarmRepository.countByMember_Id(memberId);
    }

    @Transactional
    public int alarmRead(Long memberId, Long alarmId) {
        log.info("알람 한개 읽기 memberId: "+memberId +", alarmId: "+alarmId);
        Alarm alarm = alarmRepository.findById(alarmId).orElseThrow(() -> new ApplicationException(ErrorCode.ALARM_NOT_FOUND));
        if(memberId != alarm.getMember().getId()) throw new ApplicationException(ErrorCode.INVALID_PERMISSION);
        return alarmRepository.readAlarm(memberId,alarmId);
    }

    @Transactional
    public int realAllAlarm(Long memberId) {
        log.info("알람 모두읽기 실행 memberId : " + memberId);
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new ApplicationException(ErrorCode.USERNAME_NOT_FOUNDED));
        return alarmRepository.readAllAlarm(memberId);
    }

    //무한 좋아요 방지용
    @Transactional
    public int deleteAlarm(byte category, Long postId, String senderName) {
        return alarmRepository.deleteAlarmByCategoryAndPostIdAndSenderName(category,postId,senderName);
    }
}
