# MyQ Binding

This is the MyQ binding for openHAB. It allows monitoring and control over garage doors manufactured by LiftMaster, Chamberlain and Craftsman who are compatible with the MyQ cloud service.

## Supported Things

### Account

### Garage Door

### Light

## Discovery

Once an account has been added, garage doors and lights will automatically be discovered and added to the inbox.


## Thing Configuration

_Describe what is needed to manually configure a thing, either through the (Paper) UI or via a thing-file. This should be mainly about its mandatory and optional configuration parameters. A short example entry for a thing file can help!_

_Note that it is planned to generate some part of this based on the XML files within ```src/main/resources/OH-INF/thing``` of your binding._

## Channels

| channel       | type          | description              |
|---------------|---------------|--------------------------|
| status        | String        | Status of the device     |
| switch        | Switch        | Opens (on), Closes (off) |
| contact       | contact       |                          |
| rollershutter | rollershutter |                          |

## Full Example


