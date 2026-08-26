//
//  SharedKMP+Graph.swift
//  Inkwell
//
//  Thin Swift bridges for the shared moderation XRPC/collection constants.
//

import Foundation
import InkwellShared

func sharedXrpcGraphMuteActor() -> String { XrpcEndpoints.shared.GRAPH_MUTE_ACTOR }
func sharedXrpcGraphUnmuteActor() -> String { XrpcEndpoints.shared.GRAPH_UNMUTE_ACTOR }
func sharedXrpcGraphGetMutes() -> String { XrpcEndpoints.shared.GRAPH_GET_MUTES }
func sharedXrpcGraphGetBlocks() -> String { XrpcEndpoints.shared.GRAPH_GET_BLOCKS }
func sharedXrpcModerationCreateReport() -> String { XrpcEndpoints.shared.MODERATION_CREATE_REPORT }
func sharedGraphBlockCollection() -> String { CollectionNsids.shared.GRAPH_BLOCK }
