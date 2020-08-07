package fr.gouv.stopc.robert.pushnotif.common;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class PushDate {

   private int minPushHour;

   private int maxPushHour;
   
   private String timezone;

   private Date lastPushDate;

}
