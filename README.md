# Remote App Launcher

A UEFI sample program that can pull an app from a remote server and execute it.

This is our course project for SJTU-EI6710 *Theory and Practice of UEFI*, advised by [Dr. Mi Zeyu](https://ipads.se.sjtu.edu.cn/zh/pub/members/zeyu_mi/).

## Reach Us

If you have any questions, feel free to reach out to me.

📧 gongty [at] tongji [dot] edu [dot] cn

## Modules

### Server

A kotlin web server runs on remote.

It follows a customized version of [VesperProtocol](https://github.com/FlowerBlackG/vesper/blob/main/doc/vesper-control-protocol.md), called AppServerProtocol. Both [Chinese](./RemoteAppLauncher/AppServerProtocol_ZH.md) and [English](./RemoteAppLauncher/AppServerProtocol_EN.md) versions are provided.

### DemoApps

Demo apps stored on remote, to be pulled and execute by Remote App Launcher.

### RemoteAppLauncher

Core part of this project.

It can query what server can provide, and execute them after pulling the one you want from remote.

Remote App Launcher supports command-line interface. You can check [Usage.md](./RemoteAppLauncher/Usage.md) for details.
