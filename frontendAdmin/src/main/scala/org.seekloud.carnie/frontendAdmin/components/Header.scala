//  Copyright 2018 seekloud (https://github.com/seekloud)
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.seekloud.carnie.frontendAdmin.components

/**
  * User: Jason
  * Date: 2018/12/19
  * Time: 9:51
  */
import org.seekloud.carnie.frontendAdmin.Routes.Admin
import org.seekloud.carnie.frontendAdmin.pages._
import org.seekloud.carnie.frontendAdmin.util.{Component, Http}
import org.seekloud.carnie.frontendAdmin.styles.Demo2Styles.menuHorizontalLink

import scala.xml.Elem

object Header extends Component{

  val ls = List(
    ("实时查看", CurrentDataPage.locationHashString),
    ("数据统计", ViewPage.locationHashString),
//    ("GPU预约情况", GPUOrderPage.locationHashString),
//    ("GPU使用异常", GPUOnWorkPage.locationHashString),
//    ("金币记录",CoinRecordPage.locationHashString)
  )

  def logout(): Unit = {
    val url = Admin.logout
    Http.get(url)
  }

  override def render: Elem = {
    <div>
      <div style="text-align:center;">
        <nav class="nav nav-tabs" style="text-align:center;">
          {ls.map { case (name, hash) =>
          <li>
            <a style="font-weight:bold;font-size:150%;margin:5px 15px;" class={menuHorizontalLink.htmlClass} href={hash}>
              {name}
            </a>
          </li>
        }}
          <li style="float:Right;">
            <a style="font-weight:bold;font-size:150%;margin:5px 15px;" onclick={() => logout()} class={menuHorizontalLink.htmlClass} href={"#/LoginPage"}>
              退出
            </a>
          </li>
        </nav>
      </div>
    </div>
  }


}