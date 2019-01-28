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

package org.seekloud.carnie.paperClient

import org.seekloud.carnie.util.Component

import scala.xml.Elem

class CanvasPage extends Component{

  override def render: Elem = {
    <div>
      <canvas id="RankView" tabindex="1" style="z-index: 3;position: absolute;"></canvas>
      <canvas id="GameView" tabindex="1" style="position: relative;"></canvas>
      <canvas id="BorderView" tabindex="1"></canvas>
    </div>
  }//borderView style="position: relative;"

}
