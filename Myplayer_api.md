# MyPlayer API 文档 v3.0

## 基础信息

- **协议**: HTTP/HTTPS/WS
- **数据格式**: JSON
- **认证方式**: JWT（需要在请求头中添加 `Authorization: Bearer <token>`）

## 接口列表

### 1. 用户模块

#### 1.1 用户注册

**接口地址**: `/register`  
**请求方法**: `POST`  
**请求参数**:

| 参数名        | 类型     | 描述  | 非空  |
| ---------- | ------ | --- | --- |
| u_account  | string | 账户  | 是   |
| u_password | string | 密码  | 是   |

**响应参数**:

| 参数名     | 类型      | 描述    |
| ------- | ------- | ----- |
| code    | integer | 响应状态码 |
| message | string  | 响应信息  |
| data    | string  | jwt令牌 |

#### 1.2 账户信息初始化

**接口地址**：`/setinfo`

**请求方法**：`POST`

**请求头**：

Authorization: token

**请求参数**：

| 参数名            | 类型     | 描述   | 非空  |
| -------------- | ------ | ---- | --- |
| u_name         | string | 用户名  | 是   |
| u_introduction | string | 用户签名 | 否   |

**响应参数**:

| 参数名     | 类型      | 描述    |
| ------- | ------- | ----- |
| code    | integer | 响应状态码 |
| message | string  | 响应信息  |
| data    | null    | 响应数据  |

#### 1.3 上传头像

**接口地址**：`/uploadavatar`

**请求方法**：`POST`

**请求参数**：

| 参数名 | 类型   | 描述  | 非空  |
| --- | ---- | --- | --- |
| pic | file | 图片  | 是   |

**响应参数**：

| 参数名     | 类型      | 描述      |
| ------- | ------- | ------- |
| code    | integer | 响应状态码   |
| message | string  | 响应信息    |
| data    | string  | 图片储存url |

#### 1.4 用户登录

**接口地址**: `/login`
**请求方法**: `POST`
**请求参数**:

| 参数名        | 类型     | 描述  | 非空  |
| ---------- | ------ | --- | --- |
| u_account  | string | 账号  | 是   |
| u_password | string | 密码  | 是   |

**响应参数**:

| 参数名     | 类型      | 描述     |
| ------- | ------- | ------ |
| code    | integer | 响应状态码  |
| message | string  | 响应信息   |
| data    | object  | 响应数据   |
| token   | string  | JWT 令牌 |

#### 1.5 获取用户信息

**接口地址**: `/user/getuserinfo`
**请求方法**: `GET`
**请求头**:

Authorization: token

**响应参数**:

| 参数名     | 类型      | 描述    |
| ------- | ------- | ----- |
| code    | integer | 响应状态码 |
| message | string  | 响应信息  |
| data    | object  | 响应数据  |

| 参数名            | 类型     | 描述    |
| -------------- | ------ | ----- |
| u_name         | string | 账号    |
| u_introduction | string | 签名    |
| u_avatar       | string | 头像url |

#### 1.6 修改用户名

**接口地址**：`/user/updatename`

**请求方式**：`POST`

**请求参数**：

| 参数名    | 类型     | 描述  | 非空  |
| ------ | ------ | --- | --- |
| u_name | string | 用户名 | 是   |

**响应参数**

| 参数名     | 类型      | 描述    |
| ------- | ------- | ----- |
| code    | integer | 响应状态码 |
| message | string  | 响应信息  |
| data    | null    |       |

#### 1.7 修改个人签名

**接口地址**：`/user/updateintroduction`

**请求方式**：`POST`

**请求参数**：

| 参数名            | 类型     | 描述   | 非空  |
| -------------- | ------ | ---- | --- |
| u_introduction | string | 个性签名 | 否   |

**响应参数**

| 参数名     | 类型      | 描述    |
| ------- | ------- | ----- |
| code    | integer | 响应状态码 |
| message | string  | 响应信息  |
| data    | null    |       |

#### 1.8 修改邮箱（邮箱可接收验证码）

**请求地址**：`/user/updateaccount`

**请求方式**：`POST`

**请求参数**：

| 参数名       | 类型     | 描述  | 非空  |
| --------- | ------ | --- | --- |
| u_account | string | 新邮箱 | 是   |

**响应参数**：

| 参数名     | 类型      | 描述     |
| ------- | ------- | ------ |
| code    | integer | 响应状态码  |
| message | string  | 响应信息   |
| data    | string  | 新token |

#### 1.9 修改密码

**请求地址**：`/user/updatepassword`

**请求方式**：`POST`

**请求参数**：

| 参数名         | 类型     | 描述  | 非空  |
| ----------- | ------ | --- | --- |
| oldpassword | string | 旧密码 | 是   |
| password    | string | 新密码 | 是   |

**响应参数**：

| 参数名     | 类型      | 描述    |
| ------- | ------- | ----- |
| code    | integer | 响应状态码 |
| message | string  | 响应信息  |
| data    | null    | 响应数据  |

### 2.功能模块

#### 2.1发送验证码

**接口地址**：`/sendcode`

**请求方式**：`POST`

**请求参数**：

| 参数名       | 类型     | 描述  | 非空  |
| --------- | ------ | --- | --- |
| u_account | string | 邮箱  | 是   |

**响应参数**：

| 参数名     | 类型      | 描述         |
| ------- | ------- | ---------- |
| code    | integer | 响应状态码      |
| message | string  | 响应信息       |
| data    | int     | 验证码标识码v_id |

#### 2.2验证邮箱验证码

**接口地址**：`/verifycode`

**请求方式**：`POST`

**请求参数**：

| 参数名  | 类型     | 描述     | 非空  |
| ---- | ------ | ------ | --- |
| v_id | int    | 验证码标识码 | 是   |
| code | string | 验证码    | 是   |

**响应参数**：

| 参数名     | 类型      | 描述    |
| ------- | ------- | ----- |
| code    | integer | 响应状态码 |
| message | string  | 响应信息  |
| data    | null    | 响应数据  |

#### 2.3获取好友用户在线信息

**接口地址**：`/getstatus`

**请求方式**：`POST`

**请求参数**：

| 参数名     | 类型           | 描述     |
| ------- | ------------ | ------ |
| friends | List<string> | 好友id列表 |

**响应参数**：

| 参数名     | 类型                 | 描述    |
| ------- | ------------------ | ----- |
| code    | integer            | 响应状态码 |
| message | string             | 响应信息  |
| data    | Map<string,string> | 响应数据  |

### 3.好友模块

##### 3.1 获取好友列表

**接口地址**：`/friend/getfriends`

**请求方式**：`/GET`

**请求参数**：

无

**响应参数**：

| 参数名     | 类型      | 描述    |
| ------- | ------- | ----- |
| code    | integer | 响应状态码 |
| message | string  | 响应信息  |
| data    | User    | 响应数据  |

| 参数名            | 类型     | 描述   |
| -------------- | ------ | ---- |
| u_id           | string | 好友id |
| u_avatar       | string | 好友头像 |
| u_introduction | string | 好友描述 |
| u_name         | string | 好友名字 |

##### 3.2 搜索好友

**接口地址**：`/friend/search`

**请求方式**：`/GET`

**请求参数**：

```json
params{
    u_name:input.value
}
```

**响应参数**：

| 参数名     | 类型      | 描述    |
| ------- | ------- | ----- |
| code    | integer | 响应状态码 |
| message | string  | 响应信息  |
| data    | User    | 响应数据  |

| 参数名            | 类型     | 描述   |
| -------------- | ------ | ---- |
| u_id           | string | 好友id |
| u_avatar       | string | 好友头像 |
| u_introduction | string | 好友描述 |
| u_name         | string | 好友名字 |

##### 3.3 发送申请

**接口地址**：`/inviting/sendinviting`g

**请求方式**：`POST`

**请求参数**：

| 参数名    | 类型     | 描述     | 非空  |
| ------ | ------ | ------ | --- |
| sender | string | 邀请发送方  | 是   |
| target | string | 邀请发送目标 | 是   |

**响应参数**：

| 参数名     | 类型      | 描述    |
| ------- | ------- | ----- |
| code    | integer | 响应状态码 |
| message | string  | 响应信息  |
| data    | null    | 响应数据  |

##### 3.4 接收好友申请

###### 3.4.1 获取申请表

**接口地址**：`/inviting/getinvitings`

**请求方式**：`GET`

**请求参数**：

无

**响应参数**

| 参数名     | 类型       | 描述    |
| ------- | -------- | ----- |
| code    | integer  | 响应状态码 |
| message | string   | 响应信息  |
| data    | inviting | 响应数据  |

| 参数名            | 类型       | 描述      |
| -------------- | -------- | ------- |
| inviter        | string   | 邀请人id   |
| inviter_name   | string   | 邀请人姓名   |
| inviter_avatar | string   | 邀请人头像   |
| room           | string   | 是否为房间邀请 |
| time           | datetime | 邀请时间    |

###### 3.4.2 同意邀请

**接口地址**：`/inviting/passinviting`

**请求方式**：`POST`

**请求参数**：

| 参数名     | 类型     | 描述    | 非空  |
| ------- | ------ | ----- | --- |
| inviter | string | 邀请人id | 是   |
| room    | string | 房间id  | 非   |

**响应参数**：

| 参数名     | 类型      | 描述    |
| ------- | ------- | ----- |
| code    | integer | 响应状态码 |
| message | string  | 响应信息  |
| data    | null    | 响应数据  |

###### 3.4.3 拒绝邀请

**接口地址**：`/inviting/refuseinviting`

**请求方式**：`POST`

**请求参数**：

| 参数名     | 类型     | 描述    | 非空  |
| ------- | ------ | ----- | --- |
| inviter | string | 邀请人id | 是   |
| room    | string | 房间id  | 非   |

**响应参数**：

| 参数名     | 类型      | 描述    |
| ------- | ------- | ----- |
| code    | integer | 响应状态码 |
| message | string  | 响应信息  |
| data    | null    | 响应数据  |

##### 3.5 删除好友

**接口地址**：`/friend/deletefriend`   

**请求方式**：`POST`

**请求参数**：

| 参数名  | 类型     | 描述   | 非空  |
| ---- | ------ | ---- | --- |
| u_id | string | 目标id | 是   |

**响应参数**：

| 参数名     | 类型      | 描述    |
| ------- | ------- | ----- |
| code    | integer | 响应状态码 |
| message | string  | 响应信息  |
| data    | null    | 响应数据  |

##### 3.7 选择好友进行语音聊天（待定）

### 4.私聊模块

私聊模块使用websocket一站式解决

ws://<ip>:<port>/online

ws://<ip>:<port>/voice

### 5.放映室模块

#### 放映室创建

**接口地址**：`/room/create`

**请求方式**：`POST`

**请求参数**：

| 参数名    | 类型     | 描述  | 非空  |
| ------ | ------ | --- | --- |
| r_name | string | 房间名 | 是   |

**响应参数**：

| 参数名     | 类型      | 描述    |
| ------- | ------- | ----- |
| code    | integer | 响应状态码 |
| message | string  | 响应信息  |
| data    | null    | 响应数据  |

#### 放映室搜索

**接口地址**：`/room/search`

**请求方式**：`GET`                        

**请求参数**：

```json
params:{
    name: (string input value)
}
```

**响应参数**：

| 参数名     | 类型             | 描述    |
| ------- | -------------- | ----- |
| code    | integer        | 响应状态码 |
| message | string         | 响应信息  |
| data    | List<playroom> | 响应数据  |

| 参数名            | 类型     | 描述      |
| -------------- | ------ | ------- |
| r_id           | string | 房间id    |
| r_name         | string | 房间名     |
| r_avatar       | string | 房间头像url |
| r_introduction | string | 房间介绍    |

#### 放映室加入

**接口地址**：`/inviting/sendinviting`

**请求方式**：`POST`

**请求参数**：

| 参数名    | 类型     | 描述     | 非空  |
| ------ | ------ | ------ | --- |
| sender | string | 邀请发送方  | 是   |
| target | string | 邀请发送目标 | 是   |
| room   | string | 想加入的房间 | 是   |

区分点：自己请求加入 sender==target==u_id（自己的id）

**响应参数**：

| 参数名     | 类型      | 描述    |
| ------- | ------- | ----- |
| code    | integer | 响应状态码 |
| message | string  | 响应信息  |
| data    | null    | 响应数据  |

#### 放映室加入（邀请码版本）

**接口地址**：`/room/checkinvitingcode`

**请求方式**：`POST`

**请求参数**：

```json
params:{
    code:(string ,input value, legth:6)
}
```

  **响应参数**：

| 参数名     | 类型      | 描述    |
| ------- | ------- | ----- |
| code    | integer | 响应状态码 |
| message | string  | 响应信息  |
| data    | null    | 响应数据  |

#### 放映室列表获取

**接口地址**：`/room/getrooms`

**请求方式**：`GET`

**请求参数**：无

**相应参数**：

| 参数名     | 类型             | 描述    |
| ------- | -------------- | ----- |
| code    | integer        | 响应状态码 |
| message | string         | 响应信息  |
| data    | List<Playroom> | 响应数据  |

| 参数名            | 类型     | 描述   |
| -------------- | ------ | ---- |
| r_id           | string | 房间id |
| r_name         | string | 房间名字 |
| r_avatar       | string | 房间头像 |
| r_introduction | string | 房间介绍 |

#### 放映室成员

##### 放映室成员获取

**接口地址**：`/room/getmember`

**请求方式**：`GET`

**请求参数**：

```json
params:{
    r_id: (string selected room`s id)
}
```

**响应参数**：

| 参数名     | 类型           | 描述    |
| ------- | ------------ | ----- |
| code    | integer      | 响应状态码 |
| message | string       | 响应信息  |
| data    | List<Member> | 响应数据  |

| 参数名            | 类型      | 描述   |
| -------------- | ------- | ---- |
| m_id           | string  | 成员id |
| m_name         | string  | 成员名字 |
| m_avatar       | string  | 成员头像 |
| m_introduction | string  | 成员介绍 |
| role           | integer | 成员角色 |

role： 0-拥有者  1-管理员  2-普通成员

##### 放映室成员邀请

**接口地址**：`/inviting/sendinviting`

**请求方式**：`POST`

**请求参数**：

| 参数名    | 类型     | 描述        | 非空  |
| ------ | ------ | --------- | --- |
| sender | string | 邀请发送方     | 是   |
| target | string | 邀请发送目标    | 是   |
| room   | string | 邀请加入的房间id | 是   |

**响应参数**：

| 参数名     | 类型      | 描述    |
| ------- | ------- | ----- |
| code    | integer | 响应状态码 |
| message | string  | 响应信息  |
| data    | null    | 响应数据  |

##### 放映室成员踢出（管理员权限role=0 or 1）

**接口地址**：`/room/kickmember`

**请求方式**：`POST`

**请求参数**：

```json
params：{
    m_id：（string selected member’s id）
}
```

**响应参数**：

| 参数名     | 类型      | 描述    |
| ------- | ------- | ----- |
| code    | integer | 响应状态码 |
| message | string  | 响应信息  |
| data    | null    | 响应数据  |

##### 放映室成员加入审核

###### 放映室成员加入请求获取

###### 放映室成员加入请求审核

#### 放映室信息管理

##### 放映室头像修改

##### 放映室名字修改

##### 放映室介绍修改

#### 放映室解散

#### 放映室视频相关 由websocket管理视频播放的进程、同步等等

### 6、群聊模块

#### 创建群聊

**接口地址**：`/group/create`

**请求方式**：`POST`

**请求参数**：

| 参数名    | 类型     | 描述  | 非空  |
| ------ | ------ | --- | --- |
| g_name | string | 群名称 | 否   |

**响应参数**：

| 参数名     | 类型      | 描述    |
| ------- | ------- | ----- |
| code    | integer | 响应状态码 |
| message | string  | 响应信息  |
| data    | null    | 响应数据  |

#### 搜索群聊

**接口地址**：`/group/search`

**请求方式**： `GET`

**请求参数**：

```json
params：{
    g_name：‘’
}
```

**响应参数**

| 参数名      | 类型              | 描述    |
| -------- | --------------- | ----- |
| code     | integer         | 响应状态码 |
| message  | string          | 响应信息  |
| data     | List<Groupchat> | 响应数据  |
| g_id     | string          | 群聊id  |
| g_name   | string          | 群聊名字  |
| g_note   | string          | 群公告   |
| identify | int             | 是否验证  |

#### 加入群聊

**接口地址**：`/group/joingroup`

**请求方式**：`POST`

**请求参数**：

| 参数名      | 类型     | 描述   | 非空  |
| -------- | ------ | ---- | --- |
| u_id     | string | 用户id | 是   |
| g_id     | string | 群聊id | 是   |
| identify | int    | 是否验证 | 是   |

验证群聊设置：是否可以直接加入

若identify == 1 ，则需要验证

若identify == 0 ，则不需要验证

| 参数名     | 类型      | 描述    |
| ------- | ------- | ----- |
| code    | integer | 响应状态码 |
| message | string  | 响应信息  |
| data    | null    | 响应数据  |

#### 获取群聊列表

**接口地址**：`/group/getgroups`

**请求方式**：`GET`

**请求参数**：无

**响应参数**：

| 参数名      | 类型              | 描述    |
| -------- | --------------- | ----- |
| code     | integer         | 响应状态码 |
| message  | string          | 响应信息  |
| data     | List<Groupchat> | 响应数据  |
| g_id     | string          | 群聊id  |
| g_name   | string          | 群聊名字  |
| g_note   | string          | 群公告   |
| identify | int             | 是否验证  |

#### 离开群聊

**接口地址**：`/group/leave`

**请求方式**：`GET`

**请求参数**：

```json
params：{
    g_id：‘’
}
```

**响应参数**：

| 参数名     | 类型      | 描述    |
| ------- | ------- | ----- |
| code    | integer | 响应状态码 |
| message | string  | 响应信息  |
| data    | null    | 响应数据  |

#### 群聊信息管理-增加群公告

**接口地址**：`/group/setnote`

**请求方式**：`POST`

**请求参数**：

| 参数名    | 类型     | 描述   | 非空  |
| ------ | ------ | ---- | --- |
| g_id   | string | 群聊id | 是   |
| g_note | string | 群聊公告 | 是   |

**响应参数**：

| 参数名     | 类型      | 描述    |
| ------- | ------- | ----- |
| code    | integer | 响应状态码 |
| message | string  | 响应信息  |
| data    | null    | 响应数据  |

#### 群聊信息管理-移除群公告

**接口地址**：`/group/rmnote`

**请求方式**：`POST`

**请求参数**：

| 参数名  | 类型     | 描述   | 非空  |
| ---- | ------ | ---- | --- |
| g_id | string | 群聊id | 是   |

**响应参数**：

| 参数名     | 类型      | 描述    |
| ------- | ------- | ----- |
| code    | integer | 响应状态码 |
| message | string  | 响应信息  |
| data    | null    | 响应数据  |

#### 群聊信息管理-设置头像

**接口地址**：`/avatar/upload`

**请求方式**：`POST`

**请求参数**：

```json
params：{
    file：‘type：jpg、png、gif、、、’，
    id： g_id
```

**响应参数**：

| 参数名     | 类型      | 描述    |
| ------- | ------- | ----- |
| code    | integer | 响应状态码 |
| message | string  | 响应信息  |
| data    | null    | 响应数据  |

#### 群聊信息管理-设置名字

**接口地址**：`/group/setname`

**请求方式**：`POST`

**请求参数**：

| 参数名    | 类型     | 描述   | 非空  |
| ------ | ------ | ---- | --- |
| g_id   | string | 群聊id | 是   |
| g_name | string | 名字   | 是   |

**响应参数**：

| 参数名     | 类型      | 描述    |
| ------- | ------- | ----- |
| code    | integer | 响应状态码 |
| message | string  | 响应信息  |
| data    | null    | 响应数据  |

#### 群聊信息管理-获取成员列表

**接口地址**：`/group/getmember/{g_id}`

**请求方式**：`GET`

**请求参数**：路径带参

**响应参数**：

| 参数名     | 类型           | 描述    |
| ------- | ------------ | ----- |
| code    | integer      | 响应状态码 |
| message | string       | 响应信息  |
| data    | List<Member> | 响应数据  |

#### 群聊信息管理-设置管理员MAX=4

**接口地址**：`/group/setrole`

**请求方式**：`POST`

**请求参数**：

| 参数名  | 类型     | 描述   | 非空  |
| ---- | ------ | ---- | --- |
| g_id | string | 群聊id | 是   |
| u_id | string | 目标id | 是   |
| role | int    | 设置角色 | 是   |

**响应参数**：

| 参数名     | 类型      | 描述    |
| ------- | ------- | ----- |
| code    | integer | 响应状态码 |
| message | string  | 响应信息  |
| data    | null    | 响应数据  |

#### 群聊信息管理-成员踢出

**接口地址**：`/group/kick`

**请求方式**：`POST`

**请求参数**：

| 参数名  | 类型     | 描述   | 非空  |
| ---- | ------ | ---- | --- |
| g_id | string | 群聊id | 是   |
| u_id | string | 目标id | 是   |

**响应参数**：

| 参数名     | 类型      | 描述    |
| ------- | ------- | ----- |
| code    | integer | 响应状态码 |
| message | string  | 响应信息  |
| data    | null    | 响应数据  |
