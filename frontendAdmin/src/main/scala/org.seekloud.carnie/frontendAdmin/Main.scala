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

package org.seekloud.carnie.frontendAdmin

import mhtml.{Cancelable, Rx, mount}
import org.scalajs.dom
import scala.xml.Elem
import org.seekloud.carnie.frontendAdmin.util.PageSwitcher
import org.seekloud.carnie.frontendAdmin.pages._
import org.seekloud.carnie.frontendAdmin.components.Header
import org.seekloud.carnie.frontendAdmin.styles._

/**
  * User: Jason
  * Date: 2018/12/17
  * Time: 17:27
  */
object Main {

  import scalacss.DevDefaults._
  styles.Demo2Styles.addToDocument()

  def main(args: Array[String]): Unit ={
    MainEnter.show()
  }

}

object MainEnter extends PageSwitcher {

  val currentPage: Rx[Elem] = currentHashVar.map {
    case Nil => LoginPage.render
    case "LoginPage" :: Nil => LoginPage.render
    case "View"  :: Nil => ViewPage.render
    case "CurrentDataPage"  :: Nil => CurrentDataPage.render
    case _ => <div>Error Page</div>
  }

  val header: Rx[Elem] = currentHashVar.map {
    //    case Nil => Header.render
    case "View" :: Nil => Header.render
    case "CurrentDataPage" :: Nil => Header.render
    case _ => <div></div>
  }

  def show(): Cancelable = {
    val page =
      <div>
        {header}{currentPage}
      </div>
    switchPageByHash()
    mount(dom.document.body, page)
  }

}