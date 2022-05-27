# FanboxAssistant
用于整理通过浏览器插件打包下载的fanbox作品集



## 使用说明

点击资源管理器的地址栏，输入 cmd 后回车，调起当前目录的命令行窗口

输入 ``java -jar assistant.jar [要整理的主文件夹绝对路径]`` 后回车开始执行整理

输入 ``java -jar assistant.jar help`` 查看更多帮助

在 ``whitelist.txt`` 中输入本次不需要整理的画师文件夹名称，每行输入一个名称

在 ``blacklist.txt`` 中输入本次直接清理的画师文件夹名称，每行输入一个名称

符合无用定义的文件将会保留起原始目录结构并移动到 ``★trash_bin★`` 文件夹中

确认无误清理文件后可将该文件夹删除以节省空间

