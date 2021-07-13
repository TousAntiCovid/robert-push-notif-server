1er diagramme : montrer que l'init inverse primary et secondary selon la valeur de apnHost

Les hosts des servers APNs:
* prod = api.push.apple.com
* development = api.sandbox.push.apple.com

```plantuml
@startuml

start

:configuration du primary
(config primary (apns_host));

if (config useSecondaryApns) then (yes)
if (config primary == prod) then (yes)

:initialisation du client secondary 
en pointant sur le developement;

else (no)
:initialisation du secondary 
en pointant sur la production;

endif
endif
stop

@enduml
```

2nd diagramme envoi de notification : montrer que l'APN de developement est prioritaire sur l'APN de prod.

L'envoi de notification est conditionne par l'activation du second apn client (apns_secondary_enable).

La configuration en production active ce mode de fonctionnement.

```plantuml
@startuml

start
partition "envoi de notification : **sendNotification(PushInfo, useSecondaryApns)**" {
note
en cas d'exception, mise à jour des infos de dernier échec 
(raison, date, compteur) par contre ces dernières ne sont 
potentiellement pas enregistrées (process asynchrone) !

Ce bloc de code est lancé dans un bloc asynchrone, 
lui-même appelé de façon asynchrone !!!
end note

if (useSecondaryApns 
(paramètre d'entrée = config useSecondaryAPns par défaut)) then (yes)
:envoi de la notification via le client secondaire;
else (no)
:envoi de la notification via le client primaire; 
endif

:attente de la réponse du client;

if (réponse est acceptée par la gateway) then (acceptee)
:mise à jour des infos de dernier succès (date + compteur)
mise à jour de la prochaine date de notification;  
else (refusee)
if (la raison de rejet correspond à un token device non valide :
* token non connu de la plateforme, 
* token qui ne correspond pas au topic sur lequel est envoyé la notification
voir (apns_inactive_rejection_reason)) then (yes)
if (config useSecondaryApns) then (yes)
:tentative d'envoi de la notification 
en forçant l'utilisation du primary 
(appel de **sendNotification(PushInfo, false)**); 
else (no)
:la notification est desactivee;
endif
endif
if (la raison de rejet est connue et 
on n'utilise pas le secondaryApns) then (yes)
:mise à jour des infos de dernier échec 
(raison, date, compteur);
endif
endif
:mise à jour de la notification en base de données;
}

stop

@enduml
```


Ci-dessous la configuration en fonction des plateformes

|parameter             | integration                | pre prod                  | prod                  |
|----------------------|----------------------------|---------------------------|-----------------------|
|apns_host             | api.sandbox.push.apple.com | api.push.apple.com        | api.push.apple.com    |
|apns_secondary_enable | true                       | true                      | true                  |
|apns_topic            | fr.gouv.stopcovid-int.ios  | fr.gouv.stopcovid-int.ios | fr.gouv.stopcovid.ios |

cette configuration donne donc 

|platform    | primary     | secondary   | summary |
|----------  |-------------|-----------  | --------|
|integration | development | prod        | l'application envoie les notifications en priorité sur la gateway de prod puis, si besoin, sur development        | 
|pre prod    | prod        | development | l'application envoie les notifications en priorité sur la gateway de development puis, si besoin, sur prod        |
|production  | prod        | development | l'application envoie les notifications en priorité sur la gateway de development puis, si besoin, sur prod        |

`apns_inactive_rejection_reason`: BadDeviceToken,DeviceTokenNotForTopic : même configuration quelque soit la plateforme. 
