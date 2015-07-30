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

package SocialNetwork_Example.scala

import Graph.Node
import Helpers.RandomHelper
import NodeData._
import scala.beans.BeanProperty
import scala.collection.mutable.ListBuffer

/**
 * Description: Defines all user state and transition probability information.
 * Each type is defined and mapped to a set of predicates determining the allowable parent and child types and whether or not to create them,
 * as well as the actual methods and business logic to create the parent/child states for each allowable state transition (edge/link).
 */
class UserTypes extends NodeDataTypes[User, UserStub, UserType.UserType, UserTypes] {
  def allInitialDataTypes: collection.immutable.HashSet[UserType.UserType] = {
    collection.immutable.HashSet[UserType.UserType](UserType.Admin)
  }

  def allDataTypes: collection.immutable.HashSet[UserType.UserType] = {
    collection.immutable.HashSet[UserType.UserType](UserType.Admin, UserType.SocialNetworkEmployee, UserType.PublicUser)
  }

  def dataTransitions: UserTransitions.type = UserTransitions
}

import NodeDataType.NodeDataType
object UserType {
  abstract class UserType extends NodeDataType[User, UserStub, UserTypes, UserType] {
    @BeanProperty def nodeDataTypes: UserTypes = new UserTypes()
    def asStub: UserStub = new UserStub(this)

    // We don't have any engines that use these two methods yet, but it might be useful at some point.
    override def probabilisticallyLinkToExistingParentDataNode(dataNode: Node[User]): Unit = {}
    override def probabilisticallyLinkToExistingParentStubNode(stubNode: Node[UserStub]): Unit = {}
  }

  // Admin can friend request Admin, SocialNetworkEmployee, and PublicUser
  // SocialNetworkEmployee can friend request SocialNetworkEmployee and PublicUser
  // PublicUser can friend request PublicUser

  case object Admin extends UserType {
    override def dataType: NodeDataType[User, UserStub, UserTypes, UserType] = UserType.Admin
    override val name = "Admin"

    override def getAllowableChildTypes(nodeOfThisType: Node[UserStub]): Seq[UserType.UserType] = {
      nodeDataTypes.allDataTypes.toSeq
    }
    override def getAllowableParentTypes(nodeOfThisType: Node[UserStub]): Seq[UserType.UserType] = {
      Seq[UserType.UserType](UserType.Admin)
    }
    override def childStateTransitionPredicates[T_DisplayableData <: DisplayableData](
                  node: Node[T_DisplayableData], maxToGenerate: Int, probabilityMultiplier: Int)
                  : ListBuffer[(UserType.UserType, (Node[T_DisplayableData] => Boolean))] = {
      ListBuffer[(UserType.UserType, (Node[T_DisplayableData] => Boolean))](
        (UserType.Admin, (sourceEventNode: Node[T_DisplayableData]) => RandomHelper.evaluateProbability(probabilityMultiplier*0.07)),
        (UserType.SocialNetworkEmployee, (sourceEventNode: Node[T_DisplayableData]) => RandomHelper.evaluateProbability(probabilityMultiplier*0.1)),
        (UserType.PublicUser, (sourceEventNode: Node[T_DisplayableData]) => RandomHelper.evaluateProbability(probabilityMultiplier*0.15))
      )
    }
    override def parentStateTransitionPredicates[T_DisplayableData <: DisplayableData](
                  node: Node[T_DisplayableData], maxToGenerate: Int, probabilityMultiplier: Int)
                  : ListBuffer[(UserType.UserType, (Node[T_DisplayableData] => Boolean))] = {
      ListBuffer[(UserType.UserType, (Node[T_DisplayableData] => Boolean))](
        (UserType.Admin, (sourceEventNode: Node[T_DisplayableData]) => RandomHelper.evaluateProbability(probabilityMultiplier * 0.07))
      )
    }
  }

  case object SocialNetworkEmployee extends UserType {
    override def dataType: NodeDataType[User, UserStub, UserTypes, UserType] = UserType.Admin
    override val name = "SocialNetworkEmployee"

    override def getAllowableChildTypes(nodeOfThisType: Node[UserStub]): Seq[UserType.UserType] = {
      Seq[UserType.UserType](UserType.SocialNetworkEmployee, UserType.PublicUser)
    }
    override def getAllowableParentTypes(nodeOfThisType: Node[UserStub]): Seq[UserType.UserType] = {
      Seq[UserType.UserType](UserType.Admin, UserType.SocialNetworkEmployee)
    }
    override def childStateTransitionPredicates[T_DisplayableData <: DisplayableData](
                  node: Node[T_DisplayableData], maxToGenerate: Int, probabilityMultiplier: Int)
                  : ListBuffer[(UserType.UserType, (Node[T_DisplayableData] => Boolean))] = {
      ListBuffer[(UserType.UserType, (Node[T_DisplayableData] => Boolean))](
        (UserType.SocialNetworkEmployee, (sourceEventNode: Node[T_DisplayableData]) => RandomHelper.evaluateProbability(probabilityMultiplier*0.25)),
        (UserType.PublicUser, (sourceEventNode: Node[T_DisplayableData]) => RandomHelper.evaluateProbability(probabilityMultiplier*0.30))
      )
    }
    override def parentStateTransitionPredicates[T_DisplayableData <: DisplayableData](
                  node: Node[T_DisplayableData], maxToGenerate: Int, probabilityMultiplier: Int)
                  : ListBuffer[(UserType.UserType, (Node[T_DisplayableData] => Boolean))] = {
      ListBuffer[(UserType.UserType, (Node[T_DisplayableData] => Boolean))](
        (UserType.Admin, (sourceEventNode: Node[T_DisplayableData]) => RandomHelper.evaluateProbability(probabilityMultiplier*0.03)),
        (UserType.SocialNetworkEmployee, (sourceEventNode: Node[T_DisplayableData]) => RandomHelper.evaluateProbability(probabilityMultiplier*0.25))
      )
    }
  }

  case object PublicUser extends UserType {
    override def dataType: NodeDataType[User, UserStub, UserTypes, UserType] = UserType.Admin
    override val name = "PublicUser"

    override def getAllowableChildTypes(nodeOfThisType: Node[UserStub]): Seq[UserType.UserType] = {
      Seq[UserType.UserType](UserType.PublicUser)
    }
    override def getAllowableParentTypes(nodeOfThisType: Node[UserStub]): Seq[UserType.UserType] = {
      nodeDataTypes.allDataTypes.toSeq
    }
    override def childStateTransitionPredicates[T_DisplayableData <: DisplayableData](
                  node: Node[T_DisplayableData], maxToGenerate: Int, probabilityMultiplier: Int)
                  : ListBuffer[(UserType.UserType, (Node[T_DisplayableData] => Boolean))] = {
      ListBuffer[(UserType.UserType, (Node[T_DisplayableData] => Boolean))](
        (UserType.PublicUser, (sourceEventNode: Node[T_DisplayableData]) => RandomHelper.evaluateProbability(probabilityMultiplier*0.35))
      )
    }
    override def parentStateTransitionPredicates[T_DisplayableData <: DisplayableData](
                  node: Node[T_DisplayableData], maxToGenerate: Int, probabilityMultiplier: Int)
                  : ListBuffer[(UserType.UserType, (Node[T_DisplayableData] => Boolean))]= {
      ListBuffer[(UserType.UserType, (Node[T_DisplayableData] => Boolean))](
        (UserType.Admin, (sourceEventNode: Node[T_DisplayableData]) => RandomHelper.evaluateProbability(probabilityMultiplier*0.01)),
        (UserType.SocialNetworkEmployee, (sourceEventNode: Node[T_DisplayableData]) => RandomHelper.evaluateProbability(probabilityMultiplier*0.02)),
        (UserType.PublicUser, (sourceEventNode: Node[T_DisplayableData]) => RandomHelper.evaluateProbability(probabilityMultiplier*0.35))
      )
    }
  }
}
