/*
 * Copyright 2014 DataGenerator Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package NodeData

import NodeDataType.NodeDataType
import scala.annotation.unchecked.{uncheckedVariance => uV}

/**
 * Stub of data that can be stored in a node of a graph.
 * The idea is the real data we store in a node may be way too complicated,
 * so we can use a stub to specify type and any relevant metadata (e.g., derived from a DOT
 * file, as in one implementation), and we can have an engine later expand a stub node
 * into a full-fledged data node.
 */
abstract case class NodeDataStub[+T_NodeDataType <: NodeDataType[T_NodeData, T_NodeDataStub, T_NodeDataTypes, T_NodeDataType],
                                 +T_NodeData <: NodeData,
                                 +T_NodeDataTypes <: NodeDataTypes[T_NodeData, T_NodeDataStub, T_NodeDataType, T_NodeDataTypes],
                                 +T_NodeDataStub <: NodeDataStub[T_NodeDataType, T_NodeData, T_NodeDataTypes, T_NodeDataStub]
    ] protected() extends DisplayableData {
  var dataType: (T_NodeDataType @uV)
}
